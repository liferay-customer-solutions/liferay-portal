/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.EnvironmentConstants;
import com.liferay.one.exception.DisasterRecoveryEntitlementException;
import com.liferay.one.exception.EnvironmentActivationAlreadyRequestedException;
import com.liferay.one.exception.InvalidEnvironmentAdminsException;
import com.liferay.one.model.Environment;
import com.liferay.one.util.KeyedLock;
import com.liferay.one.util.LocaleUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Franca
 */
@Component
public class CloudActivationRequestService extends OneBaseService {

	public void addActivationRequest(
			long accountEntryId, String accountExternalReferenceCode,
			long contractId, String environmentProfile,
			JSONObject fieldsJSONObject, String projectExternalReferenceCode)
		throws Exception {

		String offering = _offeringByEnvironmentProfile.get(environmentProfile);

		if (offering == null) {
			throw new IllegalArgumentException(
				"Invalid environment profile " + environmentProfile);
		}

		JSONArray adminsJSONArray = _getAdminsJSONArray(fieldsJSONObject);

		_checkAdmins(adminsJSONArray, environmentProfile);

		Environment environment =
			_environmentService.fetchActivationEnvironment(
				accountEntryId, offering, projectExternalReferenceCode);

		if (environment != null) {
			throw new EnvironmentActivationAlreadyRequestedException(
				environment.getExternalReferenceCode());
		}

		String disasterRecoveryRegion = fieldsJSONObject.optString(
			"disasterRecoveryRegion");

		if (Validator.isNotNull(disasterRecoveryRegion) &&
			!_entitlementService.hasActiveEntitlement(
				projectExternalReferenceCode,
				EntitlementConstants.NAME_DISASTER_RECOVERY)) {

			throw new DisasterRecoveryEntitlementException(
				projectExternalReferenceCode);
		}

		_copyFirstAdmin(adminsJSONArray, fieldsJSONObject);

		Environment activationEnvironment = _keyedLock.withLock(
			StringBundler.concat(
				projectExternalReferenceCode, StringPool.POUND, offering),
			() -> _addActivationEnvironment(
				accountEntryId, contractId, fieldsJSONObject, offering,
				projectExternalReferenceCode));

		try {
			_environmentAdminService.addEnvironmentAdmins(
				adminsJSONArray, activationEnvironment.getId());
		}
		catch (Exception exception) {
			_log.error(
				StringBundler.concat(
					"Unable to add the cloud activation environment admins ",
					adminsJSONArray, " to environment ",
					activationEnvironment.getExternalReferenceCode()),
				exception);
		}

		String templateExternalReferenceCode =
			_notificationTemplateERCByEnvironmentProfile.get(
				environmentProfile);

		try {
			_sendActivationRequestNotification(
				templateExternalReferenceCode,
				_getPlaceholders(
					accountExternalReferenceCode, adminsJSONArray,
					environmentProfile, fieldsJSONObject,
					projectExternalReferenceCode));
		}
		catch (Exception exception) {
			_log.error(
				"Unable to send the cloud activation notification " +
					templateExternalReferenceCode,
				exception);
		}
	}

	private Environment _addActivationEnvironment(
			long accountEntryId, long contractId, JSONObject fieldsJSONObject,
			String offering, String projectExternalReferenceCode)
		throws Exception {

		Environment environment =
			_environmentService.fetchActivationEnvironment(
				accountEntryId, offering, projectExternalReferenceCode);

		if (environment != null) {
			throw new EnvironmentActivationAlreadyRequestedException(
				environment.getExternalReferenceCode());
		}

		return _environmentService.addActivationEnvironment(
			accountEntryId, contractId, fieldsJSONObject, offering,
			projectExternalReferenceCode);
	}

	private void _checkAdmins(
			JSONArray adminsJSONArray, String environmentProfile)
		throws Exception {

		if (!environmentProfile.equals(EnvironmentConstants.PROFILE_PAAS) &&
			!environmentProfile.equals(EnvironmentConstants.PROFILE_SAAS)) {

			return;
		}

		if (adminsJSONArray.isEmpty()) {
			throw new InvalidEnvironmentAdminsException(
				StringBundler.concat(
					"At least one administrator is required for the ",
					"environment profile ", environmentProfile));
		}

		for (int i = 0; i < adminsJSONArray.length(); i++) {
			if (!(adminsJSONArray.get(i) instanceof JSONObject)) {
				throw new InvalidEnvironmentAdminsException(
					StringBundler.concat(
						"Administrator ", i,
						" is not an object for the environment profile ",
						environmentProfile));
			}
		}
	}

	private void _copyFirstAdmin(
		JSONArray adminsJSONArray, JSONObject fieldsJSONObject) {

		if (adminsJSONArray.isEmpty()) {
			return;
		}

		JSONObject adminJSONObject = adminsJSONArray.getJSONObject(0);

		fieldsJSONObject.put(
			"adminEmailAddress", adminJSONObject.optString("emailAddress")
		).put(
			"adminFirstName", adminJSONObject.optString("firstName")
		).put(
			"adminLastName", adminJSONObject.optString("lastName")
		);

		if (adminJSONObject.has("githubUsername")) {
			fieldsJSONObject.put(
				"githubUsername", adminJSONObject.optString("githubUsername"));
		}
	}

	private JSONArray _getAdminsJSONArray(JSONObject fieldsJSONObject) {
		JSONArray adminsJSONArray = fieldsJSONObject.optJSONArray("admins");

		if (adminsJSONArray == null) {
			return new JSONArray();
		}

		return adminsJSONArray;
	}

	private Map<String, String> _getAnalyticsCloudPlaceholders(
		String accountExternalReferenceCode, JSONObject fieldsJSONObject,
		String projectExternalReferenceCode) {

		return HashMapBuilder.put(
			"ALLOWED_EMAIL_DOMAINS",
			_getOptionalPlaceholderValue(
				fieldsJSONObject, "allowedEmailDomains")
		).put(
			"DATA_CENTER_LOCATION",
			HtmlUtil.escape(fieldsJSONObject.optString("region"))
		).put(
			"DATE_AND_TIME_SUBMITTED", _getDateAndTimeSubmitted()
		).put(
			"DISASTER_RECOVERY_REGION",
			_getDisasterRecoveryRegionLine(fieldsJSONObject)
		).put(
			"OWNER_EMAIL_ADDRESS",
			HtmlUtil.escape(fieldsJSONObject.optString("ownerEmailAddress"))
		).put(
			"PROJECT_ID", HtmlUtil.escape(projectExternalReferenceCode)
		).put(
			"PROJECT_SALESFORCE_ACCOUNT_LINK",
			_getSalesforceAccountLink(accountExternalReferenceCode)
		).put(
			"PROJECT_SALESFORCE_PROJECT_LINK",
			_getSalesforceProjectLink(projectExternalReferenceCode)
		).put(
			"TIME_ZONE",
			_getOptionalPlaceholderValue(fieldsJSONObject, "timeZone")
		).put(
			"WORKSPACE_FRIENDLY_URL",
			_getOptionalPlaceholderValue(fieldsJSONObject, "friendlyURL")
		).put(
			"WORKSPACE_NAME",
			HtmlUtil.escape(fieldsJSONObject.optString("workspaceName"))
		).build();
	}

	private String _getDateAndTimeSubmitted() {
		return _dateTimeFormatter.format(Instant.now());
	}

	private String _getDisasterRecoveryRegionLine(JSONObject fieldsJSONObject) {
		String disasterRecoveryRegion = fieldsJSONObject.optString(
			"disasterRecoveryRegion");

		if (Validator.isNull(disasterRecoveryRegion)) {
			return StringPool.BLANK;
		}

		return StringBundler.concat(
			"<strong>Disaster Recovery Region:</strong> ",
			HtmlUtil.escape(disasterRecoveryRegion), "<br />");
	}

	private String _getOptionalPlaceholderValue(
		JSONObject fieldsJSONObject, String key) {

		String value = fieldsJSONObject.optString(key);

		if (Validator.isNull(value)) {
			return _BLANK_TEXT;
		}

		return HtmlUtil.escape(value);
	}

	private Map<String, String> _getPaaSPlaceholders(
		JSONArray adminsJSONArray, JSONObject fieldsJSONObject,
		String projectExternalReferenceCode) {

		return HashMapBuilder.put(
			"DATE_AND_TIME_SUBMITTED", _getDateAndTimeSubmitted()
		).put(
			"DISASTER_RECOVERY_REGION",
			_getDisasterRecoveryRegionLine(fieldsJSONObject)
		).put(
			"PRIMARY_DATA_CENTER_REGION",
			HtmlUtil.escape(fieldsJSONObject.optString("region"))
		).put(
			"PROJECT_ADMIN", _getPaaSProjectAdmin(adminsJSONArray)
		).put(
			"PROJECT_EXTERNAL_REFERENCE_CODE",
			HtmlUtil.escape(projectExternalReferenceCode)
		).put(
			"PROJECT_ID",
			HtmlUtil.escape(fieldsJSONObject.optString("projectId"))
		).put(
			"PROJECT_VERSION",
			HtmlUtil.escape(fieldsJSONObject.optString("dxpVersion"))
		).build();
	}

	private String _getPaaSProjectAdmin(JSONArray adminsJSONArray) {
		StringBundler sb = new StringBundler(adminsJSONArray.length());

		for (int i = 0; i < adminsJSONArray.length(); i++) {
			JSONObject adminJSONObject = adminsJSONArray.getJSONObject(i);

			sb.append(
				StringBundler.concat(
					"<strong>Email Address - </strong> ",
					HtmlUtil.escape(adminJSONObject.optString("emailAddress")),
					"<br>\n<strong>First Name - </strong>",
					HtmlUtil.escape(adminJSONObject.optString("firstName")),
					"<br>\n<strong>Last Name - </strong>",
					HtmlUtil.escape(adminJSONObject.optString("lastName")),
					"<br>\n<strong>GitHub ID - </strong>",
					HtmlUtil.escape(
						adminJSONObject.optString("githubUsername")),
					"<br><br>"));
		}

		return sb.toString();
	}

	private Map<String, String> _getPlaceholders(
		String accountExternalReferenceCode, JSONArray adminsJSONArray,
		String environmentProfile, JSONObject fieldsJSONObject,
		String projectExternalReferenceCode) {

		if (environmentProfile.equals(
				EnvironmentConstants.PROFILE_ANALYTICS_CLOUD)) {

			return _getAnalyticsCloudPlaceholders(
				accountExternalReferenceCode, fieldsJSONObject,
				projectExternalReferenceCode);
		}

		if (environmentProfile.equals(EnvironmentConstants.PROFILE_PAAS)) {
			return _getPaaSPlaceholders(
				adminsJSONArray, fieldsJSONObject,
				projectExternalReferenceCode);
		}

		return _getSaaSPlaceholders(
			adminsJSONArray, fieldsJSONObject, projectExternalReferenceCode);
	}

	private Map<String, String> _getSaaSPlaceholders(
		JSONArray adminsJSONArray, JSONObject fieldsJSONObject,
		String projectExternalReferenceCode) {

		return HashMapBuilder.put(
			"ANALYTICS_CLOUD_OWNER_EMAIL_ADDRESS",
			HtmlUtil.escape(
				fieldsJSONObject.optString("analyticsCloudOwnerEmailAddress"))
		).put(
			"DATE_AND_TIME_SUBMITTED", _getDateAndTimeSubmitted()
		).put(
			"PRIMARY_REGION",
			HtmlUtil.escape(fieldsJSONObject.optString("region"))
		).put(
			"PROJECT_ADMIN", _getSaaSProjectAdmin(adminsJSONArray)
		).put(
			"PROJECT_EXTERNAL_REFERENCE_CODE",
			HtmlUtil.escape(projectExternalReferenceCode)
		).put(
			"PROJECT_ID",
			HtmlUtil.escape(fieldsJSONObject.optString("projectId"))
		).build();
	}

	private String _getSaaSProjectAdmin(JSONArray adminsJSONArray) {
		StringBundler sb = new StringBundler(adminsJSONArray.length());

		for (int i = 0; i < adminsJSONArray.length(); i++) {
			JSONObject adminJSONObject = adminsJSONArray.getJSONObject(i);

			sb.append(
				StringBundler.concat(
					"<strong>First Name -</strong> ",
					HtmlUtil.escape(adminJSONObject.optString("firstName")),
					"<br>\n<strong>Last Name - </strong>",
					HtmlUtil.escape(adminJSONObject.optString("lastName")),
					"<br>\n<strong>Email Address - </strong>",
					HtmlUtil.escape(adminJSONObject.optString("emailAddress")),
					"\n<br><br>"));
		}

		return sb.toString();
	}

	private String _getSalesforceAccountLink(
		String accountExternalReferenceCode) {

		if ((accountExternalReferenceCode == null) ||
			!_salesforceIdPattern.matcher(
				accountExternalReferenceCode
			).matches()) {

			return _BLANK_TEXT;
		}

		return StringBundler.concat(
			"https://liferay.lightning.force.com/lightning/r/Account/",
			accountExternalReferenceCode, "/view");
	}

	private String _getSalesforceProjectLink(
		String projectExternalReferenceCode) {

		if ((projectExternalReferenceCode == null) ||
			!_salesforceIdPattern.matcher(
				projectExternalReferenceCode
			).matches()) {

			return _BLANK_TEXT;
		}

		return StringBundler.concat(
			"https://liferay.lightning.force.com/lightning/r/Project__c/",
			projectExternalReferenceCode, "/view");
	}

	private void _sendActivationRequestNotification(
			String templateExternalReferenceCode,
			Map<String, String> placeholders)
		throws Exception {

		JSONObject processedTemplateJSONObject =
			_notificationTemplateService.getAndProcessTemplateJSONObject(
				templateExternalReferenceCode, "en_US", placeholders);

		String from = processedTemplateJSONObject.optString("from");
		String fromName = processedTemplateJSONObject.optString("fromName");
		String to = processedTemplateJSONObject.optString("to");

		if (Validator.isNull(from) || Validator.isNull(fromName) ||
			Validator.isNull(to)) {

			throw new IllegalStateException(
				StringBundler.concat(
					"Unable to resolve the cloud activation notification ",
					"sender and recipient for ",
					templateExternalReferenceCode));
		}

		_notificationQueueEntryService.addNotificationQueueEntry(
			from, fromName, to,
			processedTemplateJSONObject.optString("subject"),
			processedTemplateJSONObject.optString("body"));
	}

	private static final String _BLANK_TEXT = "< none >";

	private static final Log _log = LogFactory.getLog(
		CloudActivationRequestService.class);

	private static final DateTimeFormatter _dateTimeFormatter =
		DateTimeFormatter.ofPattern(
			"MMMM d, yyyy 'at' h:mm a 'UTC'", LocaleUtil.US
		).withZone(
			ZoneOffset.UTC
		);
	private static final Map<String, String>
		_notificationTemplateERCByEnvironmentProfile = HashMapBuilder.put(
			EnvironmentConstants.PROFILE_ANALYTICS_CLOUD,
			"SETUP-ANALYTICS-CLOUD-ENVIRONMENT"
		).put(
			EnvironmentConstants.PROFILE_PAAS, "SETUP-PAAS-ENVIRONMENT"
		).put(
			EnvironmentConstants.PROFILE_SAAS, "SETUP-SAAS-ENVIRONMENT"
		).build();
	private static final Map<String, String> _offeringByEnvironmentProfile =
		HashMapBuilder.put(
			EnvironmentConstants.PROFILE_ANALYTICS_CLOUD,
			EnvironmentConstants.OFFERING_ANALYTICS_CLOUD
		).put(
			EnvironmentConstants.PROFILE_PAAS,
			EnvironmentConstants.OFFERING_PAAS
		).put(
			EnvironmentConstants.PROFILE_SAAS,
			EnvironmentConstants.OFFERING_SAAS
		).build();
	private static final Pattern _salesforceIdPattern = Pattern.compile(
		"[A-Za-z0-9]{15}([A-Za-z0-9]{3})?");

	@Autowired
	private EntitlementService _entitlementService;

	@Autowired
	private EnvironmentAdminService _environmentAdminService;

	@Autowired
	private EnvironmentService _environmentService;

	@Autowired
	private KeyedLock _keyedLock;

	@Autowired
	private NotificationQueueEntryService _notificationQueueEntryService;

	@Autowired
	private NotificationTemplateService _notificationTemplateService;

}