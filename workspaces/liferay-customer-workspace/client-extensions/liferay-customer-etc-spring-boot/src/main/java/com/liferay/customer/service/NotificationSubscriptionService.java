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
import com.liferay.portal.kernel.util.StringUtil;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
	public void init() throws Exception {
		_siteId = _getSiteId();
	}

	@Scheduled(cron = "0 0 0 * * *")
	public void scheduled() {
		if (_log.isInfoEnabled()) {
			_log.info("Checking for notification subscriptions");
		}

		if (_announcementNotificationsEnabled) {
			try {
				_sendAnnouncementNotifications();
			}
			catch (Exception exception) {
				_log.error(
					"Error sending announcement notifications", exception);
			}
		}

		if (_businessEventNotificationsEnabled) {
			try {
				_sendBusinessEventNotifications();
			}
			catch (Exception exception) {
				_log.error(
					"Error sending business event notifications", exception);
			}
		}

		if (_licenseExpirationNotificationsEnabled) {
			try {
				_sendLicenseExpirationNotifications();
			}
			catch (Exception exception) {
				_log.error(
					"Error sending license expiration notifications",
					exception);
			}
		}

		if (_releaseNotificationsEnabled) {
			try {
				_sendReleaseNotifications();
			}
			catch (Exception exception) {
				_log.error("Error sending release notifications", exception);
			}
		}

		if (_securityVulnerabilityNotificationsEnabled) {
			try {
				_sendSecurityVulnerabilityNotifications();
			}
			catch (Exception exception) {
				_log.error(
					"Error sending security vulnerability notifications",
					exception);
			}
		}
	}

	private String _escapeFilterValue(String value) {
		if (value == null) {
			return "";
		}

		return StringUtil.replace(value, '\'', "''");
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
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

	private JSONArray _getNotificationSubscriptionsJSONArray(String filter)
		throws Exception {

		UriComponentsBuilder uriComponentsBuilder =
			UriComponentsBuilder.fromPath(
				"/o/c/notificationsubscriptions"
			).queryParam(
				"nestedFields",
				NotificationSubscriptionConstants.FIELD_NOTIFICATION_TARGET
			);

		if (filter != null) {
			uriComponentsBuilder.queryParam("filter", filter);
		}

		String json = get(
			_getAuthorization(),
			uriComponentsBuilder.build(
			).toUri());

		if ((json == null) || json.isEmpty()) {
			return new JSONArray();
		}

		try {
			JSONObject jsonObject = new JSONObject(json);

			return jsonObject.getJSONArray("items");
		}
		catch (Exception exception) {
			_log.error("Unable to parse JSON: " + json, exception);

			return new JSONArray();
		}
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

	private List<StructuredContent> _getStructuredContent(String categoryName)
		throws Exception {

		String categoryId = _getCategoryIdByName(categoryName);

		String yesterday = ZonedDateTime.now(
			ZoneOffset.UTC
		).minusDays(
			1
		).withNano(
			0
		).toInstant(
		).toString();

		String filter = String.format("dateModified ge %s", yesterday);

		StructuredContentResource structuredContentResource =
			StructuredContentResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, _getAuthorization()
			).build();

		Collection<StructuredContent> structuredContents =
			structuredContentResource.getSiteStructuredContentsPage(
				_getSiteId(), null, null, null, filter,
				com.liferay.headless.delivery.client.pagination.Pagination.of(
					1, 100),
				null
			).getItems();

		List<StructuredContent> filteredContents = new ArrayList<>();

		for (StructuredContent structuredContent : structuredContents) {
			JSONObject scJSONObject = new JSONObject(
				structuredContent.toString());

			JSONArray categoryBriefsJSONArray = scJSONObject.optJSONArray(
				"taxonomyCategoryBriefs");

			if (categoryBriefsJSONArray != null) {
				for (int i = 0; i < categoryBriefsJSONArray.length(); i++) {
					JSONObject categoryBriefJSONObject =
						categoryBriefsJSONArray.getJSONObject(i);

					if (Objects.equals(
							String.valueOf(
								categoryBriefJSONObject.getLong(
									"taxonomyCategoryId")),
							categoryId)) {

						filteredContents.add(structuredContent);

						break;
					}
				}
			}
		}

		return filteredContents;
	}

	private void _sendAnnouncementNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Sending announcement notifications");
		}

		List<StructuredContent> filteredContents = _getStructuredContent(
			NotificationSubscriptionConstants.
				CATEGORY_NAME_CUSTOMER_PORTAL_UPDATES);

		if (filteredContents.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No new announcements to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + filteredContents.size() +
					" announcements to notify");
		}

		for (StructuredContent structuredContent : filteredContents) {
			try {
				JSONObject structuredContentJSONObject = new JSONObject(
					structuredContent.toString());

				String subscriptionFilter = "type eq 'announcement'";

				JSONArray subscriptionsJSONArray =
					_getNotificationSubscriptionsJSONArray(subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"ANNOUNCEMENT_CONTENT",
					structuredContentJSONObject.optString("content", "")
				).put(
					"ANNOUNCEMENT_TITLE",
					structuredContentJSONObject.optString("title", "")
				);

				_sendNotifications(
					subscriptionsJSONArray, "ANNOUNCEMENT",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process announcement notification: " +
						structuredContent,
					exception);
			}
		}
	}

	private void _sendBusinessEventNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Sending business event notifications");
		}

		String yesterday = ZonedDateTime.now(
			ZoneOffset.UTC
		).minusDays(
			1
		).withNano(
			0
		).toInstant(
		).toString();

		try {
			JSONObject jsonObject = new JSONObject(
				get(
					_getAuthorization(),
					UriComponentsBuilder.fromPath(
						"/o/c/businessevents"
					).queryParam(
						"filter", "dateModified ge " + yesterday
					).queryParam(
						"nestedFields",
						NotificationSubscriptionConstants.
							FIELD_ACCOUNT_ENTRY_TO_BUSINESS_EVENT
					).build(
					).toUri()));

			JSONArray businessEventsJSONArray = jsonObject.getJSONArray(
				"items");

			if (businessEventsJSONArray.length() == 0) {
				if (_log.isInfoEnabled()) {
					_log.info("No new business events to notify");
				}

				return;
			}

			if (_log.isInfoEnabled()) {
				_log.info(
					"Found " + businessEventsJSONArray.length() +
						" business events to notify");
			}

			for (int i = 0; i < businessEventsJSONArray.length(); i++) {
				JSONObject businessEventJSONObject =
					businessEventsJSONArray.getJSONObject(i);

				JSONObject accountEntryJSONObject =
					businessEventJSONObject.optJSONObject(
						NotificationSubscriptionConstants.
							FIELD_ACCOUNT_ENTRY_TO_BUSINESS_EVENT);

				String externalReferenceCode = accountEntryJSONObject.getString(
					"externalReferenceCode");

				if ((externalReferenceCode == null) ||
					externalReferenceCode.isEmpty()) {

					continue;
				}

				String subscriptionFilter =
					"type eq 'businessEvent' and contains(filter, '" +
						_escapeFilterValue(externalReferenceCode) + "')";

				JSONArray subscriptionsJSONArray =
					_getNotificationSubscriptionsJSONArray(subscriptionFilter);

				if (subscriptionsJSONArray.length() == 0) {
					continue;
				}

				String id = String.valueOf(
					businessEventJSONObject.getInt("id"));

				String activityHistoryURL = String.format(
					"%s/project/%s/business-events/%s/activity-history",
					_portalUrl, externalReferenceCode, id);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"BUSINESSEVENT_ACTIVITY_HISTORY_PAGE_LINK",
					activityHistoryURL
				).put(
					"BUSINESSEVENT_EVENTTYPE",
					businessEventJSONObject.optJSONObject(
						"eventType"
					).optString(
						"key", ""
					)
				).put(
					"BUSINESSEVENT_LASTCOMMENT",
					businessEventJSONObject.optString("lastComment", "")
				).put(
					"BUSINESSEVENT_NAME",
					businessEventJSONObject.optString("name", "")
				).put(
					"BUSINESSEVENT_TARGETGOLIVEDATETIME",
					businessEventJSONObject.optString(
						"targetGoLiveDateTime", "")
				).put(
					"PROJECT_NAME", accountEntryJSONObject.optString("name", "")
				);

				_sendNotifications(
					subscriptionsJSONArray, "UPDATED-BUSINESS-EVENTS",
					templatePayloadJSONObject);
			}
		}
		catch (Exception exception) {
			_log.error(
				"Unable to process business event notifications", exception);
		}
	}

	private void _sendLicenseExpirationNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Sending license expiration notifications");
		}

		List<LicenseKey> licenseKeys =
			_provisioningService.checkLicenseExpiration(null);

		if (licenseKeys.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No expiring licenses to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + licenseKeys.size() + " expiring licenses to notify");
		}

		for (LicenseKey licenseKey : licenseKeys) {
			try {
				JSONObject licenseKeyJSONObject = new JSONObject(
					licenseKey.toString());

				String customerSegment = licenseKeyJSONObject.getString(
					"customerSegment");

				if ((customerSegment == null) || customerSegment.isEmpty()) {
					continue;
				}

				String subscriptionFilter =
					"type eq 'licenseExpiration' and filter eq '" +
						_escapeFilterValue(customerSegment) + "'";

				JSONArray subscriptionsJSONArray =
					_getNotificationSubscriptionsJSONArray(subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"LICENSE_DETAILS", licenseKeyJSONObject.toString(2));

				_sendNotifications(
					subscriptionsJSONArray, "LICENSE-EXPIRATION",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process license expiration notification: " +
						licenseKey,
					exception);
			}
		}
	}

	private void _sendNotifications(
			JSONArray subscriptionsJSONArray, String templateName,
			JSONObject templatePayloadJSONObject)
		throws Exception {

		for (int j = 0; j < subscriptionsJSONArray.length(); j++) {
			JSONObject subscriptionJSONObject =
				subscriptionsJSONArray.getJSONObject(j);

			if (!subscriptionJSONObject.getBoolean("active")) {
				continue;
			}

			JSONObject notificationTargetJSONObject =
				subscriptionJSONObject.getJSONObject(
					NotificationSubscriptionConstants.
						FIELD_NOTIFICATION_TARGET);

			JSONObject processedTemplateJSONObject =
				_notificationTemplateService.getAndProcessTemplate(
					templateName, templatePayloadJSONObject);

			String body = processedTemplateJSONObject.getString("body");
			String subject = processedTemplateJSONObject.getString("subject");

			String target = notificationTargetJSONObject.getString("target");

			_notificationQueueEntryService.addNotificationQueueEntry(
				_fromEmail, _fromName, target, subject, body);
		}
	}

	private void _sendReleaseNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Sending release notifications");
		}

		List<StructuredContent> filteredContents = _getStructuredContent(
			NotificationSubscriptionConstants.CATEGORY_NAME_RELEASE_NOTES);

		if (filteredContents.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No new release notes to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + filteredContents.size() +
					" release notes to notify");
		}

		for (StructuredContent structuredContent : filteredContents) {
			try {
				JSONObject structuredContentJSONObject = new JSONObject(
					structuredContent.toString());

				String productVersion = structuredContentJSONObject.getString(
					"productVersion");

				if ((productVersion == null) || productVersion.isEmpty()) {
					continue;
				}

				String subscriptionFilter =
					"type eq 'release' and filter eq '" +
						_escapeFilterValue(productVersion) + "'";

				JSONArray subscriptionsJSONArray =
					_getNotificationSubscriptionsJSONArray(subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"RELEASE_CONTENT",
					structuredContentJSONObject.optString("content", "")
				).put(
					"RELEASE_TITLE",
					structuredContentJSONObject.optString("title", "")
				);

				_sendNotifications(
					subscriptionsJSONArray, "RELEASE",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process release notification: " +
						structuredContent,
					exception);
			}
		}
	}

	private void _sendSecurityVulnerabilityNotifications() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Sending security vulnerability notifications");
		}

		String jql = String.format(
			"project = %s AND created >= \"-1d\"",
			_jiraSecurityVulnerabilityProject);

		List<JiraSupportIssue> jiraSupportIssues = _jiraService.search(
			jql,
			new String[] {"key", "status", "summary", "labels", "ticketURL"});

		if (jiraSupportIssues.isEmpty()) {
			if (_log.isInfoEnabled()) {
				_log.info("No new security vulnerabilities to notify");
			}

			return;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Found " + jiraSupportIssues.size() +
					" security vulnerabilities to notify");
		}

		for (JiraSupportIssue issue : jiraSupportIssues) {
			try {
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

				String audience = "Customer";

				if ((issue.getLabels() != null) &&
					Arrays.asList(
						issue.getLabels()
					).contains(
						"Partner"
					)) {

					audience = "Partner";
				}

				String subscriptionFilter = String.format(
					"type eq 'securityVulnerability' and filter eq '%s'",
					_escapeFilterValue(audience));

				JSONArray subscriptionsJSONArray =
					_getNotificationSubscriptionsJSONArray(subscriptionFilter);

				JSONObject templatePayloadJSONObject = new JSONObject();

				templatePayloadJSONObject.put(
					"VULNERABILITY_KEY", issueJSONObject.optString("key", "")
				).put(
					"VULNERABILITY_SUMMARY",
					issueJSONObject.optString("summary", "")
				).put(
					"VULNERABILITY_TICKET_URL",
					issueJSONObject.optString("ticketURL", "")
				);

				_sendNotifications(
					subscriptionsJSONArray, "SECURITY-VULNERABILITY",
					templatePayloadJSONObject);
			}
			catch (Exception exception) {
				_log.error(
					"Unable to process security vulnerability notification: " +
						issue,
					exception);
			}
		}
	}

	private static final Log _log = LogFactory.getLog(
		NotificationSubscriptionService.class);

	@Value("${liferay.customer.notification.subscription.announcement.enabled}")
	private boolean _announcementNotificationsEnabled;

	@Value(
		"${liferay.customer.notification.subscription.business.event.enabled}"
	)
	private boolean _businessEventNotificationsEnabled;

	@Value("${liferay.customer.portal.friendly.url.path}")
	private String _friendlyUrlPath;

	@Value("${liferay.customer.notification.subscription.from.email}")
	private String _fromEmail;

	@Value("${liferay.customer.notification.subscription.from.name}")
	private String _fromName;

	@Value("${liferay.customer.jira.security.vulnerability.project}")
	private String _jiraSecurityVulnerabilityProject;

	@Autowired
	private JiraService _jiraService;

	@Value(
		"${liferay.customer.notification.subscription.license.expiration.enabled}"
	)
	private boolean _licenseExpirationNotificationsEnabled;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private NotificationTemplateService _notificationTemplateService;

	@Value("${liferay.customer.portal.url}")
	private String _portalUrl;

	@Autowired
	private ProvisioningService _provisioningService;

	@Value("${liferay.customer.notification.subscription.release.enabled}")
	private boolean _releaseNotificationsEnabled;

	@Value(
		"${liferay.customer.notification.subscription.security.vulnerability.enabled}"
	)
	private boolean _securityVulnerabilityNotificationsEnabled;

	private Long _siteId;

}