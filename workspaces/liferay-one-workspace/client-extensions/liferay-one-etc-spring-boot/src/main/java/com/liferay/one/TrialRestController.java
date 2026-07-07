/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.portal.instances.client.dto.v1_0.Admin;
import com.liferay.headless.portal.instances.client.dto.v1_0.PortalInstance;
import com.liferay.headless.portal.instances.client.pagination.Page;
import com.liferay.headless.portal.instances.client.resource.v1_0.PortalInstanceResource;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.ConsoleService;
import com.liferay.one.service.NotificationQueueEntryService;
import com.liferay.one.service.NotificationTemplateService;
import com.liferay.one.service.UserAccountService;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Keven Leone
 */
@RequestMapping("/trial")
@RestController
public class TrialRestController extends BaseRestController {

	@DeleteMapping("{orderId}")
	public void deleteTrial(@PathVariable long orderId) throws Exception {
		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		JSONObject trialProvisioningContextJSONObject =
			_getTrialProvisioningContextJSONObject(order);

		_consoleService.deleteProject(
			trialProvisioningContextJSONObject.getString("projectId"));

		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		String virtualHost = null;

		if (customFields != null) {
			virtualHost = customFields.get("trial-virtual-host");
		}

		_deletePortalInstance(
			orderId, trialProvisioningContextJSONObject, virtualHost);
	}

	@GetMapping("availability")
	public String getAvailability(
			@RequestParam(defaultValue = "SOLUTIONS7", required = false) String
				orderTypeExternalReferenceCode)
		throws Exception {

		Page<PortalInstance> portalInstancesPage = _getPortalInstancesPage(
			_getTrialProvisioningContextJSONObject(
				_getOrder(orderTypeExternalReferenceCode)));

		return new JSONObject(
		).put(
			"active", _trialMaxInstances > portalInstancesPage.getTotalCount()
		).put(
			"available",
			_trialMaxInstances - portalInstancesPage.getTotalCount()
		).put(
			"max", _trialMaxInstances
		).toString();
	}

	@GetMapping("domain-availability/{projectPrefix}")
	public ResponseEntity<Void> getDomainAvailability(
			@PathVariable String projectPrefix,
			@RequestParam(defaultValue = "SSA_SAAS", required = false) String
				orderTypeExternalReferenceCode)
		throws Exception {

		JSONObject trialProvisioningContextJSONObject =
			_getTrialProvisioningContextJSONObject(
				_getOrder(orderTypeExternalReferenceCode));

		String virtualHost =
			projectPrefix + "." +
				trialProvisioningContextJSONObject.getString("domain");

		Page<PortalInstance> portalInstancesPage = _getPortalInstancesPage(
			trialProvisioningContextJSONObject);

		for (PortalInstance portalInstance : portalInstancesPage.getItems()) {
			if (Objects.equals(virtualHost, portalInstance.getVirtualHost())) {
				return ResponseEntity.status(
					HttpStatus.CONFLICT
				).build();
			}
		}

		return ResponseEntity.status(
			HttpStatus.OK
		).build();
	}

	@PostMapping("expire/{orderId}")
	public void postExpire(@PathVariable long orderId) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Expired trial " + orderId);
		}

		_commerceOrderService.completeOrder(
			orderId, CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);

		deleteTrial(orderId);
	}

	@PostMapping("extend/{id}")
	public void postExtend(@PathVariable long id) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Extend trial " + id);
		}

		JSONObject trialExtensionRequestJSONObject = new JSONObject(
			get(
				_liferayOAuth2AccessTokenManager.getAuthorization(
					"liferay-one-etc-spring-boot-oahs"),
				UriComponentsBuilder.fromPath(
					"/o/c/trialextensionrequests/" + id
				).build(
				).toUri()));

		JSONObject dueStatusJSONObject =
			trialExtensionRequestJSONObject.getJSONObject("dueStatus");

		if (!(Objects.equals(
				dueStatusJSONObject.getString("key"), "Approved") ||
			  Objects.equals(
				  dueStatusJSONObject.getString("key"), "AutoApproved") ||
			  Objects.equals(
				  dueStatusJSONObject.getString("key"), "Pending"))) {

			return;
		}

		long orderId = trialExtensionRequestJSONObject.getLong(
			"r_orderToTrialExtensionRequest_commerceOrderId");

		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		String trialEndDate = null;

		if (customFields != null) {
			trialEndDate = customFields.get("trial-end-date");
		}

		if (Validator.isNull(trialEndDate)) {
			throw new IllegalStateException(
				"Order " + orderId + " has no \"trial-end-date\" custom field");
		}

		if (Objects.equals(dueStatusJSONObject.getString("key"), "Pending")) {
			patch(
				_liferayOAuth2AccessTokenManager.getAuthorization(
					"liferay-one-etc-spring-boot-oahs"),
				new JSONObject(
				).put(
					"dueStatus", "Approved"
				).toString(),
				UriComponentsBuilder.fromPath(
					"/o/c/trialextensionrequests/" + id
				).build(
				).toUri());
		}

		customFields.put(
			"trial-end-date",
			ZonedDateTime.parse(
				trialEndDate
			).plusDays(
				trialExtensionRequestJSONObject.getInt("duration")
			).format(
				DateTimeFormatter.ISO_INSTANT
			));

		_commerceOrderService.updateOrder(
			customFields, orderId, order.getOrderStatus());
	}

	@PostMapping("provisioning/{orderId}")
	public void postProvisioningOrder(@PathVariable long orderId)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info("Provisioning order " + orderId);
		}

		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		JSONObject trialProvisioningContextJSONObject =
			_getTrialProvisioningContextJSONObject(order);

		Page<PortalInstance> portalInstancesPage = _getPortalInstancesPage(
			trialProvisioningContextJSONObject);

		if (portalInstancesPage.getTotalCount() >= _trialMaxInstances) {
			_log.error("Order is on hold");

			_commerceOrderService.updateOrder(
				null, orderId, CommerceOrderConstants.ORDER_STATUS_ON_HOLD);

			return;
		}

		if (order.getOrderStatus() ==
				CommerceOrderConstants.ORDER_STATUS_OPEN) {

			_commerceOrderService.updateOrder(
				null, orderId, CommerceOrderConstants.ORDER_STATUS_PENDING);
		}

		_commerceOrderService.updateOrder(
			null, orderId, CommerceOrderConstants.ORDER_STATUS_PROCESSING);

		UserAccount userAccount =
			_userAccountService.getUserAccountByEmailAddress(
				order.getCreatorEmailAddress());

		if (userAccount == null) {
			throw new IllegalArgumentException(
				"No user account exists for email address \"" +
					order.getCreatorEmailAddress() + "\"");
		}

		JSONObject trialSettingsJSONObject = _getTrialSettingsJSONObject(order);

		boolean sendNotificationEmail = trialSettingsJSONObject.optBoolean(
			"sendNotificationEmail", true);

		if (sendNotificationEmail) {
			_postNotification(
				order.getCreatorEmailAddress(), "TRIAL-PROCESSING-ORDER",
				HashMapBuilder.put(
					"COMMERCEORDER_AUTHOR_FIRST_NAME",
					userAccount.getGivenName()
				).put(
					"COMMERCEORDER_ID", String.valueOf(orderId)
				).build());
		}

		PortalInstance portalInstance = null;

		try {
			portalInstance = _postPortalInstance(
				userAccount, order.getCreatorEmailAddress(),
				trialSettingsJSONObject.optString(
					"projectId", String.valueOf(orderId)),
				trialSettingsJSONObject.optString("siteInitializerKey", null),
				trialProvisioningContextJSONObject);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to provision portal instance for order " + orderId,
				exception);

			_commerceOrderService.updateOrder(
				null, orderId, CommerceOrderConstants.ORDER_STATUS_CANCELLED);

			throw exception;
		}

		try {
			_consoleService.setUpProject(
				trialProvisioningContextJSONObject.getString("cluster"),
				trialProvisioningContextJSONObject.getBoolean("deployable"),
				trialProvisioningContextJSONObject.getString("dxpProjectUid"),
				portalInstance.getVirtualHost(),
				_toStringArray(
					trialSettingsJSONObject.optJSONArray(
						"consoleInviteEmailAddresses", new JSONArray())),
				orderId,
				trialProvisioningContextJSONObject.getString("projectId"));

			_commerceOrderService.updateOrder(
				HashMapBuilder.put(
					"trial-end-date",
					ZonedDateTime.now(
					).plusDays(
						trialSettingsJSONObject.optInt("duration", 7)
					).format(
						DateTimeFormatter.ISO_INSTANT
					)
				).put(
					"trial-start-date",
					ZonedDateTime.now(
					).format(
						DateTimeFormatter.ISO_INSTANT
					)
				).put(
					"trial-virtual-host", portalInstance.getVirtualHost()
				).build(),
				orderId, CommerceOrderConstants.ORDER_STATUS_IN_PROGRESS);

			if (sendNotificationEmail) {
				_postNotification(
					order.getCreatorEmailAddress(), "TRIAL-COMPLETED-ORDER",
					HashMapBuilder.put(
						"EMAIL", order.getCreatorEmailAddress()
					).put(
						"NAME", userAccount.getGivenName()
					).put(
						"URL", portalInstance.getVirtualHost()
					).build());
			}
		}
		catch (WebClientResponseException webClientResponseException) {
			_rollBackTrial(
				webClientResponseException.getResponseBodyAsString(), orderId,
				portalInstance, trialProvisioningContextJSONObject);
		}
		catch (Exception exception) {
			_rollBackTrial(
				exception.getMessage(), orderId, portalInstance,
				trialProvisioningContextJSONObject);
		}
	}

	@Scheduled(cron = "0 0 */6 * * *")
	protected void scheduledProcessTrials() {
		try {
			_processInProgressTrials();
		}
		catch (Exception exception) {
			_log.error("Unable to process in progress trials", exception);
		}

		try {
			_processOnHoldTrials();
		}
		catch (Exception exception) {
			_log.error("Unable to process on hold trials", exception);
		}
	}

	private void _deletePortalInstance(
			long orderId, JSONObject trialProvisioningContextJSONObject,
			String virtualHost)
		throws Exception {

		if (Validator.isNull(virtualHost)) {
			return;
		}

		PortalInstanceResource portalInstanceResource =
			_getPortalInstanceResource(trialProvisioningContextJSONObject);

		Page<PortalInstance> portalInstancesPage =
			portalInstanceResource.getPortalInstancesPage(true);

		for (PortalInstance portalInstance : portalInstancesPage.getItems()) {
			if (Objects.equals(portalInstance.getVirtualHost(), virtualHost)) {
				portalInstanceResource.deletePortalInstance(
					portalInstance.getPortalInstanceId());

				break;
			}
		}

		if (_log.isInfoEnabled()) {
			_log.info("Portal instance deleted for order " + orderId);
		}
	}

	private Order _getOrder(String orderTypeExternalReferenceCode) {
		Order order = new Order();

		order.setCustomFields(() -> new HashMap<>());
		order.setOrderTypeExternalReferenceCode(
			() -> orderTypeExternalReferenceCode);

		return order;
	}

	private PortalInstanceResource _getPortalInstanceResource(
			JSONObject trialProvisioningContextJSONObject)
		throws Exception {

		return PortalInstanceResource.builder(
		).endpoint(
			new URI(
				trialProvisioningContextJSONObject.getString("trialHomePageURL")
			).toURL()
		).header(
			HttpHeaders.AUTHORIZATION,
			_liferayOAuth2AccessTokenManager.getAuthorization(
				trialProvisioningContextJSONObject.getString(
					"trialAuthorizationERC"))
		).build();
	}

	private Page<PortalInstance> _getPortalInstancesPage(
			JSONObject trialProvisioningContextJSONObject)
		throws Exception {

		PortalInstanceResource portalInstanceResource =
			_getPortalInstanceResource(trialProvisioningContextJSONObject);

		return portalInstanceResource.getPortalInstancesPage(true);
	}

	private JSONObject _getTrialProvisioningContextJSONObject(Order order) {
		JSONObject trialSettingsJSONObject = _getTrialSettingsJSONObject(order);

		String projectId = trialSettingsJSONObject.optString(
			"projectId", String.valueOf(order.getId()));

		if (Objects.equals(
				order.getOrderTypeExternalReferenceCode(), "SOLUTIONS7")) {

			return new JSONObject(
			).put(
				"cluster", _consoleTrialCluster
			).put(
				"deployable", true
			).put(
				"domain", _trialDXPDomain
			).put(
				"dxpProjectUid", _consoleTrialProjectUid
			).put(
				"projectId", _consoleTrialProjectPrefix + "-ext" + projectId
			).put(
				"trialAuthorizationERC", "external-trial"
			).put(
				"trialHomePageURL", _externalTrialHomePageURL
			);
		}

		if (Objects.equals(
				order.getOrderTypeExternalReferenceCode(), "SSA_SAAS")) {

			return new JSONObject(
			).put(
				"cluster", _consoleSSACluster
			).put(
				"deployable", false
			).put(
				"domain", _trialSSADXPDomain
			).put(
				"dxpProjectUid", _consoleSSAProjectUid
			).put(
				"projectId", _consoleSSAProjectPrefix + "-ext" + projectId
			).put(
				"trialAuthorizationERC", "external-ssa"
			).put(
				"trialHomePageURL", _externalSSAHomePageURL
			);
		}

		throw new IllegalArgumentException(
			"Unsupported order type: " +
				order.getOrderTypeExternalReferenceCode());
	}

	private JSONObject _getTrialSettingsJSONObject(Order order) {
		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		if (customFields == null) {
			return new JSONObject();
		}

		return new JSONObject(
			customFields.getOrDefault("trial-settings", "{}"));
	}

	private void _notifyEnd(long orderId) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Notify end " + orderId);
		}

		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		Map<String, String> customFields =
			(Map<String, String>)order.getCustomFields();

		String trialEndDate = null;

		if (customFields != null) {
			trialEndDate = customFields.get("trial-end-date");
		}

		if (Validator.isNull(trialEndDate)) {
			throw new IllegalStateException(
				"Order " + orderId + " has no \"trial-end-date\" custom field");
		}

		UserAccount userAccount =
			_userAccountService.getUserAccountByEmailAddress(
				order.getCreatorEmailAddress());

		_postNotification(
			order.getCreatorEmailAddress(), "TRIAL-EXPIRING-ORDER",
			HashMapBuilder.put(
				"TRIAL_CREATOR_FIRST_NAME", userAccount.getGivenName()
			).put(
				"TRIAL_END_DATE",
				ZonedDateTime.parse(
					trialEndDate
				).format(
					DateTimeFormatter.ofPattern("MMMM d, yyyy", LocaleUtil.US)
				)
			).build());

		customFields.put(
			"trial-notify-end-date",
			ZonedDateTime.now(
			).format(
				DateTimeFormatter.ISO_INSTANT
			));

		_commerceOrderService.updateOrder(
			customFields, orderId, order.getOrderStatus());
	}

	private void _postNotification(
		String toEmailAddress, String externalReferenceCode,
		Map<String, String> placeholders) {

		try {
			JSONObject processedTemplateJSONObject =
				_notificationTemplateService.getAndProcessTemplateJSONObject(
					externalReferenceCode, "en_US", placeholders);

			_notificationQueueEntryService.addNotificationQueueEntry(
				"customer-service@liferay.com", "Liferay Support",
				toEmailAddress,
				processedTemplateJSONObject.getString("subject"),
				processedTemplateJSONObject.getString("body"));
		}
		catch (Exception exception) {
			_log.error(
				StringBundler.concat(
					"Unable to send notification ", externalReferenceCode,
					" to ", toEmailAddress),
				exception);
		}
	}

	private PortalInstance _postPortalInstance(
			UserAccount userAccount, String emailAddress, String projectId,
			String siteInitializerKey,
			JSONObject trialProvisioningContextJSONObject)
		throws Exception {

		PortalInstanceResource portalInstanceResource =
			_getPortalInstanceResource(trialProvisioningContextJSONObject);

		PortalInstance portalInstance = new PortalInstance();

		Admin admin = new Admin();

		admin.setEmailAddress(() -> emailAddress);
		admin.setFamilyName(
			() -> Objects.toString(
				userAccount.getFamilyName(), StringPool.BLANK));
		admin.setGivenName(
			() -> Objects.toString(
				userAccount.getGivenName(), StringPool.BLANK));

		portalInstance.setAdmin(() -> admin);

		portalInstance.setDomain(() -> "lxc.app");
		portalInstance.setSiteInitializerKey(() -> siteInitializerKey);

		String domain =
			projectId + "." +
				trialProvisioningContextJSONObject.getString("domain");

		portalInstance.setPortalInstanceId(() -> domain);
		portalInstance.setVirtualHost(() -> domain);

		portalInstance = portalInstanceResource.postPortalInstance(
			portalInstance);

		if (_log.isInfoEnabled()) {
			_log.info("Created portal instance " + portalInstance);
		}

		return portalInstance;
	}

	private void _processInProgressTrials() throws Exception {
		List<Order> orders = _commerceOrderService.getOrders(
			StringBundler.concat(
				"orderStatus/any(x:(x eq ",
				CommerceOrderConstants.ORDER_STATUS_IN_PROGRESS,
				")) and orderTypeExternalReferenceCode in ('SSA_SAAS', ",
				"'SOLUTIONS7')"));

		for (Order order : orders) {
			long orderId = order.getId();

			try {
				Map<String, String> customFields =
					(Map<String, String>)order.getCustomFields();

				if (customFields == null) {
					continue;
				}

				String trialEndDate = customFields.get("trial-end-date");

				if (Validator.isNull(trialEndDate)) {
					continue;
				}

				ZonedDateTime nowZonedDateTime = ZonedDateTime.now();

				ZonedDateTime trialEndDateZonedDateTime = ZonedDateTime.parse(
					trialEndDate);

				if (nowZonedDateTime.isAfter(trialEndDateZonedDateTime)) {
					postExpire(orderId);

					continue;
				}

				String trialNotifyEndDate = customFields.get(
					"trial-notify-end-date");

				if (Validator.isNull(trialNotifyEndDate) &&
					!nowZonedDateTime.isBefore(
						trialEndDateZonedDateTime.minusDays(1))) {

					_notifyEnd(orderId);
				}
			}
			catch (Exception exception) {
				_log.error("Unable to process order " + orderId, exception);
			}
		}
	}

	private void _processOnHoldTrials() throws Exception {
		List<Order> orders = _commerceOrderService.getOrders(
			StringBundler.concat(
				"orderStatus/any(x:(x eq ",
				CommerceOrderConstants.ORDER_STATUS_ON_HOLD,
				")) and orderTypeExternalReferenceCode eq 'SOLUTIONS7'"));

		if (orders.isEmpty()) {
			return;
		}

		JSONObject availabilityJSONObject = new JSONObject(
			getAvailability("SOLUTIONS7"));

		if (!availabilityJSONObject.getBoolean("active")) {
			if (_log.isInfoEnabled()) {
				_log.info("There are no available seats");
			}

			return;
		}

		long available = availabilityJSONObject.getLong("available");

		for (Order order : orders) {
			if (available <= 0) {
				if (_log.isInfoEnabled()) {
					_log.info("There are no available seats");
				}

				break;
			}

			long orderId = order.getId();

			try {
				if (_log.isInfoEnabled()) {
					_log.info("Processing on hold order " + orderId);
				}

				postProvisioningOrder(orderId);

				if (_log.isInfoEnabled()) {
					_log.info("Processed on hold order " + orderId);
				}

				available--;
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process on hold order " + orderId, exception);
			}
		}
	}

	private void _rollBackTrial(
			String errorMessage, long orderId, PortalInstance portalInstance,
			JSONObject trialProvisioningContextJSONObject)
		throws Exception {

		_log.error(
			StringBundler.concat(
				"Unable to set up project for order ", orderId, ": \n",
				errorMessage));

		String virtualHost = null;

		if (portalInstance != null) {
			virtualHost = portalInstance.getVirtualHost();
		}

		try {
			_consoleService.deleteProject(
				trialProvisioningContextJSONObject.getString("projectId"));
		}
		catch (Exception exception) {
			_log.error(
				"Unable to delete project during rollback for order " + orderId,
				exception);
		}

		try {
			_deletePortalInstance(
				orderId, trialProvisioningContextJSONObject, virtualHost);
		}
		catch (Exception exception) {
			_log.error(
				"Unable to delete portal instance during rollback for order " +
					orderId,
				exception);
		}

		_commerceOrderService.updateOrder(
			HashMapBuilder.put(
				"trial-error", errorMessage
			).put(
				"trial-error-date",
				ZonedDateTime.now(
				).format(
					DateTimeFormatter.ISO_INSTANT
				)
			).put(
				"trial-virtual-host", virtualHost
			).build(),
			orderId, CommerceOrderConstants.ORDER_STATUS_CANCELLED);
	}

	private String[] _toStringArray(JSONArray jsonArray) {
		List<String> list = new ArrayList<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			list.add(jsonArray.getString(i));
		}

		return list.toArray(new String[0]);
	}

	private static final Log _log = LogFactory.getLog(
		TrialRestController.class);

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ConsoleService _consoleService;

	@Value("${liferay.one.console.ssa.cluster}")
	private String _consoleSSACluster;

	@Value("${liferay.one.console.ssa.project.prefix}")
	private String _consoleSSAProjectPrefix;

	@Value("${liferay.one.console.ssa.project.uid}")
	private String _consoleSSAProjectUid;

	@Value("${liferay.one.console.cluster}")
	private String _consoleTrialCluster;

	@Value("${liferay.one.console.project.prefix}")
	private String _consoleTrialProjectPrefix;

	@Value("${liferay.one.console.project.uid}")
	private String _consoleTrialProjectUid;

	@Value("${external.ssa.oauth2.headless.server.home.page.url:}")
	private String _externalSSAHomePageURL;

	@Value("${external.trial.oauth2.headless.server.home.page.url:}")
	private String _externalTrialHomePageURL;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private NotificationTemplateService _notificationTemplateService;

	@Value("${liferay.one.trial.dxp.domain}")
	private String _trialDXPDomain;

	@Value("${liferay.one.trial.max.instances}")
	private int _trialMaxInstances;

	@Value("${liferay.one.trial.ssa.dxp.domain}")
	private String _trialSSADXPDomain;

	@Autowired
	private UserAccountService _userAccountService;

}