/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.client.extension.util.spring.boot3.service.BaseService;
import com.liferay.customer.constants.NotificationSubscriptionConstants;
import com.liferay.customer.model.JiraSupportIssue;
import com.liferay.headless.admin.taxonomy.client.dto.v1_0.TaxonomyCategory;
import com.liferay.headless.admin.taxonomy.client.pagination.Pagination;
import com.liferay.headless.admin.taxonomy.client.resource.v1_0.TaxonomyCategoryResource;
import com.liferay.headless.admin.user.client.dto.v1_0.Site;
import com.liferay.headless.admin.user.client.resource.v1_0.SiteResource;
import com.liferay.headless.delivery.client.dto.v1_0.StructuredContent;
import com.liferay.headless.delivery.client.resource.v1_0.StructuredContentResource;
import com.liferay.osb.provisioning.rest.client.dto.v1_0.LicenseKey;
import com.liferay.portal.kernel.util.StringBundler;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Ryan Schuhler
 */
@Component
public class NotificationSubscriptionService extends BaseService {

	@PostConstruct
	public void init() {
		try {
			_siteId = _getSiteId();
		}
		catch (Exception exception) {
			_log.error("Unable to get site id", exception);
		}
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void scheduled() {
		try {
			_checkNotificationSubscriptions();
		}
		catch (Exception exception) {
			_log.error(
				"Unable to check for notification subscriptions", exception);
		}
	}

	private void _checkNotificationSubscriptions() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Checking for notification subscriptions");
		}

		JSONArray notificationSubscriptionsJSONArray =
			_getNotificationSubscriptionsJSONArray();

		for (int i = 0; i < notificationSubscriptionsJSONArray.length(); i++) {
			JSONObject notificationSubscriptionJSONObject =
				notificationSubscriptionsJSONArray.getJSONObject(i);

			if (notificationSubscriptionJSONObject.getBoolean("active")) {
				String filterField =
					notificationSubscriptionJSONObject.getString("filter");

				String typeField =
					notificationSubscriptionJSONObject.getJSONObject(
						"type"
					).getString(
						"key"
					);

				NotificationInfo notificationInfo = null;

				if (typeField.equals("announcement")) {
					notificationInfo = _getAnnouncementNotificationInfo(
						filterField);
				}
				else if (typeField.equals("businessEvent")) {
					notificationInfo = _getBusinessEventNotificationInfo(
						filterField);
				}
				else if (typeField.equals("licenseExpiration")) {
					notificationInfo = _getLicenseExpirationNotificationInfo(
						filterField);
				}
				else if (typeField.equals("release")) {
					notificationInfo = _getReleaseNotificationInfo(filterField);
				}
				else if (typeField.equals("securityVulnerability")) {
					notificationInfo =
						_getSecurityVulnerabilityNotificationInfo(filterField);
				}
				else {
					if (_log.isWarnEnabled()) {
						_log.warn("Unknown notification type: " + typeField);
					}

					continue;
				}

				if (notificationInfo == null) {
					continue;
				}

				long notificationTargetId =
					notificationSubscriptionJSONObject.getLong(
						NotificationSubscriptionConstants.
							FIELD_NOTIFICATION_TARGET);

				_notificationQueueEntryService.addNotificationQueueEntry(
					_fromEmail, _fromName,
					_getNotificationTarget(notificationTargetId),
					notificationInfo.getSubject(), notificationInfo.getBody());
			}
		}
	}

	private NotificationInfo _getAnnouncementNotificationInfo(
			String filterField)
		throws Exception {

		String categoryId = _getCategoryIdByName(
			NotificationSubscriptionConstants.
				CATEGORY_NAME_CUSTOMER_PORTAL_UPDATES);

		String prefixedFilter = new StringBundler(
			5
		).append(
			"categoryId eq "
		).append(
			categoryId
		).append(
			" and ("
		).append(
			filterField
		).append(
			")"
		).toString();

		StructuredContentResource structuredContentResource =
			StructuredContentResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, _getAuthorization()
			).build();

		Collection<StructuredContent> structuredContents =
			structuredContentResource.getSiteStructuredContentsPage(
				_getSiteId(), null, null, null, prefixedFilter,
				com.liferay.headless.delivery.client.pagination.Pagination.of(
					1, 100),
				null
			).getItems();

		if (structuredContents.isEmpty()) {
			return null;
		}

		JSONArray jsonArray = new JSONArray();

		for (StructuredContent structuredContent : structuredContents) {
			jsonArray.put(new JSONObject(structuredContent.toString()));
		}

		String response = jsonArray.toString(2);

		return new NotificationInfo(
			"Customer Portal Updates",
			"The following announcements have been published: \n\n" + response);
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private NotificationInfo _getBusinessEventNotificationInfo(
			String filterField)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			get(
				_getAuthorization(),
				UriComponentsBuilder.fromPath(
					"/o/c/businessevents"
				).queryParam(
					"filter", filterField
				).build(
				).toUri()));

		JSONArray jsonArray = jsonObject.getJSONArray("items");

		if (jsonArray.length() == 0) {
			return null;
		}

		String response = jsonArray.toString(2);

		return new NotificationInfo(
			"Business Events",
			"The following business events have occurred: \n\n" + response);
	}

	private String _getCategoryIdByName(String categoryName) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Searching for category: " + categoryName);
		}

		TaxonomyCategoryResource taxonomyCategoryResource =
			TaxonomyCategoryResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, _getAuthorization()
			).build();

		Collection<TaxonomyCategory> taxonomyCategories =
			taxonomyCategoryResource.getSiteTaxonomyCategoriesPage(
				Long.valueOf(_siteId), null, null,
				"name eq '" + categoryName + "'", Pagination.of(1, 1), null
			).getItems();

		if (!taxonomyCategories.isEmpty()) {
			return taxonomyCategories.iterator(
			).next(
			).getId();
		}

		throw new Exception("Category not found: " + categoryName);
	}

	private NotificationInfo _getLicenseExpirationNotificationInfo(
		String filterField) {

		if (_log.isInfoEnabled()) {
			_log.info(
				"Calling Provisioning Service for License Expiration: " +
					filterField);
		}

		List<LicenseKey> licenseKeys =
			_provisioningService.checkLicenseExpiration(filterField);

		if (licenseKeys.isEmpty()) {
			return null;
		}

		String response = new JSONArray(
			licenseKeys
		).toString(
			2
		);

		return new NotificationInfo(
			"License Expiration Report",
			"The following licenses are expiring soon: \n\n" + response);
	}

	private JSONArray _getNotificationSubscriptionsJSONArray()
		throws Exception {

		String json = get(
			_getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/notificationsubscriptions"
			).build(
			).toUri());

		if ((json == null) || json.isEmpty()) {
			return new JSONArray();
		}

		JSONObject jsonObject = new JSONObject(json);

		return jsonObject.getJSONArray("items");
	}

	private String _getNotificationTarget(Long id) throws Exception {
		String json = get(
			_getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/notificationtargets/" + id
			).build(
			).toUri());

		if ((json == null) || json.isEmpty()) {
			throw new Exception(
				"Unable to get notification target for id: " + id);
		}

		JSONObject notificationTargetJSONObject = new JSONObject(json);

		return notificationTargetJSONObject.getString("target");
	}

	private NotificationInfo _getReleaseNotificationInfo(String filterField)
		throws Exception {

		String categoryId = _getCategoryIdByName(
			NotificationSubscriptionConstants.CATEGORY_NAME_RELEASE_NOTES);

		String prefixedFilter = new StringBundler(
			5
		).append(
			"categoryId eq "
		).append(
			categoryId
		).append(
			" and ("
		).append(
			filterField
		).append(
			")"
		).toString();

		StructuredContentResource structuredContentResource =
			StructuredContentResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, _getAuthorization()
			).build();

		Collection<StructuredContent> structuredContents =
			structuredContentResource.getSiteStructuredContentsPage(
				_getSiteId(), null, null, null, prefixedFilter,
				com.liferay.headless.delivery.client.pagination.Pagination.of(
					1, 100),
				null
			).getItems();

		if (structuredContents.isEmpty()) {
			return null;
		}

		JSONArray jsonArray = new JSONArray();

		for (StructuredContent structuredContent : structuredContents) {
			jsonArray.put(new JSONObject(structuredContent.toString()));
		}

		String response = jsonArray.toString(2);

		return new NotificationInfo(
			"Release Notes",
			"The following release notes have been published: \n\n" + response);
	}

	private NotificationInfo _getSecurityVulnerabilityNotificationInfo(
			String filterField)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info(
				"Calling Jira Service for Security Vulnerability with JQL: " +
					filterField);
		}

		List<JiraSupportIssue> jiraSupportIssues = _jiraService.search(
			filterField,
			new String[] {"key", "status", "summary", "labels", "ticketURL"});

		if (jiraSupportIssues.isEmpty()) {
			return null;
		}

		JSONArray jsonArray = new JSONArray();

		for (JiraSupportIssue issue : jiraSupportIssues) {
			JSONObject issueJSONObject = new JSONObject();

			issueJSONObject.put(
				"key", issue.getKey()
			).put(
				"labels", new JSONArray(issue.getLabels())
			).put(
				"status", issue.getStatus()
			).put(
				"summary", issue.getSummary()
			).put(
				"ticketURL", issue.getTicketURL()
			);

			jsonArray.put(issueJSONObject);
		}

		String response = jsonArray.toString(2);

		return new NotificationInfo(
			"Security Vulnerability",
			"The following security vulnerabilities have been found: \n\n" +
				response);
	}

	private Long _getSiteId() throws Exception {
		if (_siteId != null) {
			return _siteId;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Searching for site with friendly URL path: " +
					_friendlyUrlPath);
		}

		SiteResource.Builder builder = SiteResource.builder();

		SiteResource siteResource = builder.endpoint(
			_lxcDXPMainDomain, _lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, _getAuthorization()
		).build();

		Site site = siteResource.getSiteByFriendlyUrlPath(_friendlyUrlPath);

		if (site != null) {
			_siteId = site.getId();

			return _siteId;
		}

		throw new Exception("Site not found: " + _friendlyUrlPath);
	}

	private static final Log _log = LogFactory.getLog(
		NotificationSubscriptionService.class);

	@Value("${liferay.customer.portal.friendly.url.path}")
	private String _friendlyUrlPath;

	@Value("${liferay.customer.notification.subscription.from.email}")
	private String _fromEmail;

	@Value("${liferay.customer.notification.subscription.from.name}")
	private String _fromName;

	@Autowired
	private JiraService _jiraService;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private ProvisioningService _provisioningService;

	private Long _siteId;

	private static class NotificationInfo {

		public NotificationInfo(String subject, String body) {
			_subject = subject;
			_body = body;
		}

		public String getBody() {
			return _body;
		}

		public String getSubject() {
			return _subject;
		}

		private final String _body;
		private final String _subject;

	}

}