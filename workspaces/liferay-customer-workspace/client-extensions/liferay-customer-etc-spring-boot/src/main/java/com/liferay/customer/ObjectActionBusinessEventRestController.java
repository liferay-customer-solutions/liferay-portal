/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.customer.constants.NotificationTemplateERCConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Felipe Franca
 */
@RestController
public class ObjectActionBusinessEventRestController
	extends BaseRestController {

	@RequestMapping(
		method = RequestMethod.POST, path = "/object/action/business/event"
	)
	public ResponseEntity<String> post(@RequestBody String json) {
		JSONObject jsonObject = new JSONObject(json);

		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
			"objectEntryDTOBusinessEvent");

		JSONObject businessEventPropertiesJSONObject =
			businessEventJSONObject.getJSONObject("properties");

		JSONObject businessEventVersionJSONObject = new JSONObject(
		).put(
			"change",
			_getChangeJSONObject(jsonObject, businessEventPropertiesJSONObject)
		).put(
			"comment",
			_getComment(jsonObject, businessEventPropertiesJSONObject)
		).put(
			"r_accountEntryToBusinessEventVersions_accountEntryId",
			businessEventPropertiesJSONObject.getString(
				"r_accountEntryToBusinessEvents_accountEntryId")
		).put(
			"r_businessEventToBusinessEventVersions_c_businessEventId",
			businessEventJSONObject.getString("id")
		);

		try {
			post(
				_getAuthorization(), businessEventVersionJSONObject.toString(),
				"/o/c/businesseventversions");
		}
		catch (Exception exception) {
			StringBundler sb = new StringBundler(2);

			sb.append("Unable to create business event version:\n");
			sb.append(businessEventVersionJSONObject.toString());

			_log.error(sb.toString(), exception);

			return new ResponseEntity(
				exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			JSONObject koroneikiAccountJSONObject = _getKoroneikiAccount(
				businessEventPropertiesJSONObject.getString(
					"accountEntryToBusinessEventsERC"));

			_sendNotification(
				businessEventJSONObject, businessEventPropertiesJSONObject,
				jsonObject, koroneikiAccountJSONObject);
		}
		catch (Exception exception) {
			_log.error(exception);

			return new ResponseEntity(
				exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private JSONObject _getChangeJSONObject(
		JSONObject jsonObject, JSONObject propertiesJSONObject) {

		if (_isNewEntry(jsonObject)) {
			return new JSONObject(
			).put(
				"key", "created"
			).put(
				"name", "Created"
			);
		}

		if (_isCanceledEvent(propertiesJSONObject)) {
			return new JSONObject(
			).put(
				"key", "eventCanceled"
			).put(
				"name", "Event Canceled"
			);
		}

		if (_isGoLive(propertiesJSONObject)) {
			return new JSONObject(
			).put(
				"key", "goLive"
			).put(
				"name", "Go Live"
			);
		}

		return new JSONObject(
		).put(
			"key", "edited"
		).put(
			"name", "Edited"
		);
	}

	private String _getComment(
		JSONObject jsonObject, JSONObject propertiesJSONObject) {

		if (_isNewEntry(jsonObject)) {
			return "New business event has been created.";
		}

		return propertiesJSONObject.optString("lastComment");
	}

	private String _getCXLeadEmail(String region) throws Exception {
		String emailName =
			region.toUpperCase(
			).replace(
				" ", "_"
			) + "_CX_LEAD";

		return _getEmailByName(emailName);
	}

	private String _getEmailByName(String name) throws Exception {
		if (name.equals("AUSTRALIA_CX_LEAD")) {
			return _australiaCXLead;
		}
		else if (name.equals("AUSTRALIA_RSM")) {
			return _australiaRSM;
		}
		else if (name.equals("BRAZIL_CX_LEAD")) {
			return _brazilCXLead;
		}
		else if (name.equals("BRAZIL_RSM")) {
			return _brazilRSM;
		}
		else if (name.equals("CHINA_CX_LEAD")) {
			return _chinaCXLead;
		}
		else if (name.equals("CHINA_RSM")) {
			return _chinaRSM;
		}
		else if (name.equals("GLOBAL_CX_LEAD")) {
			return _globalCXLead;
		}
		else if (name.equals("GLOBAL_RSM")) {
			return _globalRSM;
		}
		else if (name.equals("HUNGARY_CX_LEAD")) {
			return _hungaryCXLead;
		}
		else if (name.equals("HUNGARY_RSM")) {
			return _hungaryRSM;
		}
		else if (name.equals("INDIA_CX_LEAD")) {
			return _indiaCXLead;
		}
		else if (name.equals("INDIA_RSM")) {
			return _indiaRSM;
		}
		else if (name.equals("JAPAN_CX_LEAD")) {
			return _japanCXLead;
		}
		else if (name.equals("JAPAN_RSM")) {
			return _japanRSM;
		}
		else if (name.equals("SPAIN_CX_LEAD")) {
			return _spainCXLead;
		}
		else if (name.equals("SPAIN_RSM")) {
			return _spainRSM;
		}
		else if (name.equals("UNITED_STATES_CX_LEAD")) {
			return _unitedStatesCXLead;
		}
		else if (name.equals("UNITED_STATES_RSM")) {
			return _unitedStatesRSM;
		}

		StringBundler sb = new StringBundler(2);

		sb.append("No email found for name ");
		sb.append(name);

		throw new Exception(sb.toString());
	}

	private JSONObject _getKoroneikiAccount(String externalReferenceCode)
		throws Exception {

		return new JSONObject(
			get(
				_getAuthorization(),
				"/o/c/koroneikiaccounts/by-external-reference-code/" +
					externalReferenceCode));
	}

	private JSONObject _getKoroneikiAccountJSONObject(
		String externalReferenceCode) {

		String url =
			"/o/c/koroneikiaccounts/by-external-reference-code/" +
				externalReferenceCode;

		return new JSONObject(get(_getAuthorization(), url));
	}

	private String _getNotificationTemplateERC(
		JSONObject jsonObject, JSONObject propertiesJSONObject) {

		JSONObject changeJSONObject = _getChangeJSONObject(
			jsonObject, propertiesJSONObject);

		String changeKey = changeJSONObject.getString("key");

		if (StringUtil.equalsIgnoreCase(changeKey, "created")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_CREATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equalsIgnoreCase(changeKey, "edited")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_UPDATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equalsIgnoreCase(changeKey, "eventCanceled")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_CANCELED_NOTIFICATION_TEMPLATE;
		}

		return NotificationTemplateERCConstants.
			BUSINESS_EVENT_COMPLETED_NOTIFICATION_TEMPLATE;
	}

	private JSONObject _getNotificationTemplateJSONObject(String erc) {
		String url =
			"/o/notification/v1.0/notification-templates/by-external-reference-code/" +
				erc;

		return new JSONObject(get(_getAuthorization(), url));
	}

	private JSONObject _getPayloadJSONObject(
			JSONObject businessEventJSONObject,
			JSONObject businessEventPropertiesJSONObject,
			JSONObject koroneikiAccountJSONObject,
			JSONObject notificationTemplateJSONObject)
		throws Exception {

		JSONObject notificationTemplateBodyJSONObject =
			notificationTemplateJSONObject.getJSONObject("body");

		String notificationTemplateBody = _parseString(
			businessEventJSONObject, businessEventPropertiesJSONObject,
			koroneikiAccountJSONObject,
			notificationTemplateBodyJSONObject.getString("en_US"));

		JSONObject notificationTemplateSubjectJSONObject =
			notificationTemplateJSONObject.getJSONObject("subject");

		String notificationTemplateSubject = _parseString(
			businessEventJSONObject, businessEventPropertiesJSONObject,
			koroneikiAccountJSONObject,
			notificationTemplateSubjectJSONObject.getString("en_US"));

		return new JSONObject(
		).put(
			"body", notificationTemplateBody
		).put(
			"recipients",
			_parseRecipientsJSONArray(
				koroneikiAccountJSONObject,
				notificationTemplateJSONObject.getJSONArray("recipients"))
		).put(
			"subject", notificationTemplateSubject
		).put(
			"type", "email"
		);
	}

	private String _getRecipientsTo(JSONObject koroneikiAccountJSONObject)
		throws Exception {

		boolean hasTAMServiceSubscription = _hasTAMServiceSubscription(
			koroneikiAccountJSONObject.getString("accountKey"));

		String region = koroneikiAccountJSONObject.getString("region");

		String rsmEmail = _getRSMEmail(region);

		if (hasTAMServiceSubscription) {
			String cxLeadEmail = _getCXLeadEmail(region);

			String recipientsTo = rsmEmail + ", " + cxLeadEmail;

			return recipientsTo;
		}

		return rsmEmail;
	}

	private String _getRSMEmail(String region) throws Exception {
		String emailName =
			region.toUpperCase(
			).replace(
				" ", "_"
			) + "_RSM";

		return _getEmailByName(emailName);
	}

	private boolean _hasTAMServiceSubscription(String externalReferenceCode) {
		String url =
			"/o/c/accountsubscriptions/?filter=(name eq 'Technical Account Management Services' or name eq 'Technical Account Management Services - LATAM') and accountKey eq '" +
				externalReferenceCode + "'";

		JSONObject accountSubscriptionsJSONObject = new JSONObject(
			get(_getAuthorization(), url));

		JSONArray accountSubscriptionsJSONArray =
			accountSubscriptionsJSONObject.getJSONArray("items");

		if (accountSubscriptionsJSONArray.length() > 0) {
			return true;
		}

		return false;
	}

	private boolean _isCanceledEvent(JSONObject jsonObject) {
		JSONObject eventStatusJSONObject = jsonObject.getJSONObject(
			"eventStatus");

		return StringUtil.equalsIgnoreCase(
			eventStatusJSONObject.getString("key"), "canceled");
	}

	private boolean _isGoLive(JSONObject jsonObject) {
		return !StringUtil.equalsIgnoreCase(
			jsonObject.optString("actualGoLiveDateTime"), "");
	}

	private boolean _isNewEntry(JSONObject jsonObject) {
		return StringUtil.equalsIgnoreCase(
			jsonObject.getString("objectActionTriggerKey"), "onAfterAdd");
	}

	private JSONArray _parseRecipientsJSONArray(
			JSONObject koroneikiAccountJSONObject,
			JSONArray recipientsJSONArray)
		throws Exception {

		JSONObject recipientJSONObject = recipientsJSONArray.getJSONObject(0);

		JSONObject recipientFromName = recipientJSONObject.getJSONObject(
			"fromName");

		String recipientsTo = _getRecipientsTo(koroneikiAccountJSONObject);

		recipientJSONObject.put(
			"fromName", recipientFromName.getString("en_US")
		).put(
			"to", recipientsTo
		);

		return new JSONArray(
		).put(
			recipientJSONObject
		);
	}

	private String _parseString(
		JSONObject businessEventJSONObject,
		JSONObject businessEventPropertiesJSONObject,
		JSONObject koroneikiAccountJSONObject, String string) {

		StringBundler sb = new StringBundler(2);

		sb.append("https://support.liferay.com/project/#/");
		sb.append(
			businessEventPropertiesJSONObject.getString(
				"accountEntryToBusinessEventsERC") + "/business-events/" +
					businessEventJSONObject.getString("id"));

		String parsedString;

		parsedString = StringUtil.replace(
			string, "[%BUSINESS_EVENT_LINK]", sb.toString());

		parsedString = StringUtil.replace(
			parsedString, "[%EVENT_NAME]",
			businessEventPropertiesJSONObject.getString("name"));

		JSONObject eventTypeJSONObject =
			businessEventPropertiesJSONObject.getJSONObject("eventType");

		parsedString = StringUtil.replace(
			parsedString, "[%EVENT_TYPE]",
			eventTypeJSONObject.getString("name"));

		parsedString = StringUtil.replace(
			parsedString, "[%PROJECT_NAME]",
			koroneikiAccountJSONObject.getString("name"));

		String feedback = businessEventPropertiesJSONObject.optString(
			"feedback", "");

		if (StringUtil.equalsIgnoreCase(feedback, "")) {
			parsedString = StringUtil.removeSubstring(
				parsedString, "[%SUPPORT_FEEDBACK_IF_ANY]");
		}
		else {
			parsedString = StringUtil.replace(
				parsedString, "[%SUPPORT_FEEDBACK_IF_ANY]",
				"<p>" + feedback + "</p>");
		}

		String targetGoLiveDateTime =
			businessEventPropertiesJSONObject.getString("targetGoLiveDateTime");

		parsedString = StringUtil.replace(
			parsedString, "[%TARGET_GO_LIVE_DATE]",
			targetGoLiveDateTime.split("T")[0]);

		String reason = businessEventPropertiesJSONObject.optString(
			"lastComment", "");

		if (StringUtil.equalsIgnoreCase(reason, "")) {
			parsedString = StringUtil.removeSubstring(
				parsedString, "[%REASON_IF_ANY]");
		}
		else {
			parsedString = StringUtil.replace(
				parsedString, "[%REASON_IF_ANY]", "<p>" + reason + "</p>");
		}

		return parsedString;
	}

	private void _sendNotification(
			JSONObject businessEventJSONObject,
			JSONObject businessEventPropertiesJSONObject, JSONObject jsonObject,
			JSONObject koroneikiAccountJSONObject)
		throws Exception {

		String notificationTemplateERC = _getNotificationTemplateERC(
			jsonObject, businessEventPropertiesJSONObject);

		JSONObject notificationTemplateJSONObject =
			_getNotificationTemplateJSONObject(notificationTemplateERC);

		if (notificationTemplateJSONObject.isEmpty()) {
			String errorMessage =
				"No template found for external reference code " +
					notificationTemplateERC;

			throw new Exception(errorMessage);
		}

		JSONObject payloadJSONObject = _getPayloadJSONObject(
			businessEventJSONObject, businessEventPropertiesJSONObject,
			koroneikiAccountJSONObject, notificationTemplateJSONObject);

		post(
			_getAuthorization(), payloadJSONObject.toString(),
			"/o/notification/v1.0/notification-queue-entries");
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionBusinessEventRestController.class);

	@Value("${liferay.customer.emails.australia.cx.lead}")
	private String _australiaCXLead;

	@Value("${liferay.customer.emails.australia.rsm}")
	private String _australiaRSM;

	@Value("${liferay.customer.emails.brazil.cx.lead}")
	private String _brazilCXLead;

	@Value("${liferay.customer.emails.brazil.rsm}")
	private String _brazilRSM;

	@Value("${liferay.customer.emails.china.cx.lead}")
	private String _chinaCXLead;

	@Value("${liferay.customer.emails.china.rsm}")
	private String _chinaRSM;

	@Value("${liferay.customer.emails.global.cx.lead}")
	private String _globalCXLead;

	@Value("${liferay.customer.emails.global.rsm}")
	private String _globalRSM;

	@Value("${liferay.customer.emails.hungary.cx.lead}")
	private String _hungaryCXLead;

	@Value("${liferay.customer.emails.hungary.rsm}")
	private String _hungaryRSM;

	@Value("${liferay.customer.emails.india.cx.lead}")
	private String _indiaCXLead;

	@Value("${liferay.customer.emails.india.rsm}")
	private String _indiaRSM;

	@Value("${liferay.customer.emails.japan.cx.lead}")
	private String _japanCXLead;

	@Value("${liferay.customer.emails.japan.rsm}")
	private String _japanRSM;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${liferay.customer.emails.spain.cx.lead}")
	private String _spainCXLead;

	@Value("${liferay.customer.emails.spain.rsm}")
	private String _spainRSM;

	@Value("${liferay.customer.emails.united_states.cx.lead}")
	private String _unitedStatesCXLead;

	@Value("${liferay.customer.emails.united_states.rsm}")
	private String _unitedStatesRSM;

}