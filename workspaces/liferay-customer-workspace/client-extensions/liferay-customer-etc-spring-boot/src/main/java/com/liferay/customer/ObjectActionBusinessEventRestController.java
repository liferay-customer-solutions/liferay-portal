/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.customer.constants.EmailsConstants;
import com.liferay.customer.constants.NotificationTemplateERCConstants;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

	@RequestMapping(method = RequestMethod.POST, path = "/object/action/business/event")
	public ResponseEntity<String> post(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {
		JSONObject jsonObject = new JSONObject(json);

		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
				"objectEntryDTOBusinessEvent");

		JSONObject businessEventPropertiesJSONObject = businessEventJSONObject.getJSONObject("properties");

		JSONObject businessEventVersionJSONObject = new JSONObject().put(
				"change",
				_getChangeJSONObject(jsonObject, businessEventPropertiesJSONObject)).put(
						"comment",
						_getComment(jsonObject, businessEventPropertiesJSONObject))
				.put(
						"r_accountEntryToBusinessEventVersions_accountEntryId",
						businessEventPropertiesJSONObject.getString(
								"r_accountEntryToBusinessEvents_accountEntryId"))
				.put(
						"r_businessEventToBusinessEventVersions_c_businessEventId",
						businessEventJSONObject.getString("id"));

		try {
			post(
					"Bearer " + jwt.getTokenValue(),
					businessEventVersionJSONObject.toString(),
					"/o/c/businesseventversions");
		} catch (Exception exception) {
			StringBundler sb = new StringBundler(4);

			sb.append("Unable to create business event version:\n");
			sb.append(businessEventVersionJSONObject.toString());
			sb.append("\nAuthor's ID: ");
			sb.append(jwt.getClaimAsString("sub"));

			_log.error(sb.toString(), exception);

			return new ResponseEntity(
					exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		try {
			JSONObject koroneikiAccountJSONObject = _getKoroneikiAccount(businessEventPropertiesJSONObject.getString("accountEntryToBusinessEventsERC"), jwt);

			_sendNotification(businessEventJSONObject, businessEventPropertiesJSONObject, jsonObject, koroneikiAccountJSONObject, jwt);
		} catch (Exception exception) {
			_log.error(exception);

			return new ResponseEntity(
					exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private JSONObject _getChangeJSONObject(
			JSONObject jsonObject, JSONObject propertiesJSONObject) {

		if (_isNewEntry(jsonObject)) {
			return new JSONObject().put(
					"key", "created").put(
							"name", "Created");
		}

		if (_isCanceledEvent(propertiesJSONObject)) {
			return new JSONObject().put(
					"key", "eventCanceled").put(
							"name", "Event Canceled");
		}

		if (_isGoLive(propertiesJSONObject)) {
			return new JSONObject().put(
					"key", "goLive").put(
							"name", "Go Live");
		}

		return new JSONObject().put(
				"key", "edited").put(
						"name", "Edited");
	}

	private String _getComment(
			JSONObject jsonObject, JSONObject propertiesJSONObject) {

		if (_isNewEntry(jsonObject)) {
			return "New business event has been created.";
		}

		return propertiesJSONObject.optString("lastComment");
	}

	private String _getCXLeadEmail(String region) {
		return EmailsConstants.EMAILS_MAP.get(region.toUpperCase().replace(" ", "_") + "_CX_LEAD");
	}

	private JSONObject _getKoroneikiAccount (String externalReferenceCode, Jwt jwt) {
		return new JSONObject(
				get(
						"Bearer " + jwt.getTokenValue(),
						"/o/c/koroneikiaccounts/by-external-reference-code/"
								+ externalReferenceCode));
	}

	private String _getNotificationTemplateERC(JSONObject jsonObject, JSONObject propertiesJSONObject) {
		JSONObject changeJSONObject = _getChangeJSONObject(jsonObject, propertiesJSONObject);

		String changeKey = changeJSONObject.getString("key");

		if (StringUtil.equalsIgnoreCase(changeKey, "created")) {
			return NotificationTemplateERCConstants.BUSINESS_EVENT_CREATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equalsIgnoreCase(changeKey, "edited")) {
			return NotificationTemplateERCConstants.BUSINESS_EVENT_UPDATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equalsIgnoreCase(changeKey, "eventCanceled")) {
			return NotificationTemplateERCConstants.BUSINESS_EVENT_CANCELED_NOTIFICATION_TEMPLATE;
		}

		return NotificationTemplateERCConstants.BUSINESS_EVENT_COMPLETED_NOTIFICATION_TEMPLATE;

	}

	private JSONObject _getNotificationTemplateJSONObject(String externalReferenceCode, Jwt jwt) {
		return new JSONObject(
				get(
						"Bearer " + jwt.getTokenValue(),
						"/o/notification/v1.0/notification-templates/by-external-reference-code/"
								+ externalReferenceCode));
	}

	private JSONObject _getPayloadJSONObject(JSONObject businessEventJSONObject, JSONObject businessEventPropertiesJSONObject, JSONObject koroneikiAccountJSONObject, JSONObject notificationTemplateJSONObject, Jwt jwt) {
		JSONObject notificationTemplateBodyJSONObject = notificationTemplateJSONObject.getJSONObject("body");

		String notificationTemplateBody = _parseString(businessEventJSONObject, businessEventPropertiesJSONObject, koroneikiAccountJSONObject, notificationTemplateBodyJSONObject.getString("en_US"));

		JSONObject notificationTemplateSubjectJSONObject = notificationTemplateJSONObject.getJSONObject("subject");

		String notificationTemplateSubject = _parseString(businessEventJSONObject, businessEventPropertiesJSONObject, koroneikiAccountJSONObject, notificationTemplateSubjectJSONObject.getString("en_US"));

		return new JSONObject().put(
			"body", notificationTemplateBody).put(
			"recipients", _parseRecipientsJSONArray(koroneikiAccountJSONObject, notificationTemplateJSONObject.getJSONArray("recipients"), jwt)).put(
			"subject", notificationTemplateSubject).put(
			"type", "email");
	}

	private JSONArray _parseRecipientsJSONArray(JSONObject koroneikiAccountJSONObject, JSONArray recipientsJSONArray, Jwt jwt) {
		JSONObject recipientJSONObject = recipientsJSONArray.getJSONObject(0);
		JSONObject recipientFromName = recipientJSONObject.getJSONObject("fromName");

		recipientJSONObject.put("fromName", recipientFromName.getString("en_US"));
		recipientJSONObject.put("to", _getRecipientsTo(koroneikiAccountJSONObject, jwt));
		
		return new JSONArray().put(recipientJSONObject);
	}

	private String _getRecipientsTo(JSONObject koroneikiAccountJSONObject, Jwt jwt) {
		boolean hasTAMServiceSubscription = _hasTAMServiceSubscription(koroneikiAccountJSONObject.getString("accountKey"), jwt);

		if (hasTAMServiceSubscription) {
			return _getRSMEmail(koroneikiAccountJSONObject.getString("region")) + ", " + _getCXLeadEmail(koroneikiAccountJSONObject.getString("region"));
		}

		return _getRSMEmail(koroneikiAccountJSONObject.getString("region"));
	}

	private String _getRSMEmail(String region) {
		return EmailsConstants.EMAILS_MAP.get(region.toUpperCase().replace(" ", "_") + "_RSM");
	}

	private boolean _hasTAMServiceSubscription(String externalReferenceCode, Jwt jwt) {
		JSONObject accountSubscriptionsJSONObject = new JSONObject(
			get(
					"Bearer " + jwt.getTokenValue(),
					"/o/c/accountsubscriptions/?filter=(name eq 'Technical Account Management Services' or name eq 'Technical Account Management Services - LATAM') and accountKey eq '"
							+ externalReferenceCode + "'"));
		
		JSONArray accountSubscriptionsJSONArray = accountSubscriptionsJSONObject.getJSONArray("items");

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

	private String _parseString(JSONObject businessEventJSONObject, JSONObject businessEventPropertiesJSONObject, JSONObject koroneikiAccountJSONObject, String string) {
		StringBundler sb = new StringBundler(4);

		sb.append("https://support.liferay.com/project/#/");
		sb.append(businessEventPropertiesJSONObject.getString("accountEntryToBusinessEventsERC"));
		sb.append("/business-events/");
		sb.append(businessEventJSONObject.getString("id"));

		String parsedString;

		parsedString = StringUtil.replace(string, "[%BUSINESS_EVENT_LINK]", sb.toString());

		parsedString = StringUtil.replace(parsedString, "[%EVENT_NAME]", businessEventPropertiesJSONObject.getString("name"));

		JSONObject eventTypeJSONObject = businessEventPropertiesJSONObject.getJSONObject("eventType");

		parsedString = StringUtil.replace(parsedString, "[%EVENT_TYPE]", eventTypeJSONObject.getString("name"));

		parsedString = StringUtil.replace(parsedString, "[%PROJECT_NAME]", koroneikiAccountJSONObject.getString("name"));

		String feedback = businessEventPropertiesJSONObject.optString("feedback", "");

		if (StringUtil.equalsIgnoreCase(feedback, "")){
			parsedString = StringUtil.replace(parsedString, "[%SUPPORT_FEEDBACK_IF_ANY]", "");
		}
		else {
			parsedString = StringUtil.replace(parsedString, "[%SUPPORT_FEEDBACK_IF_ANY]", "<p>" + feedback + "</p>");
		}
		
		String targetGoLiveDateTime = businessEventPropertiesJSONObject.getString("targetGoLiveDateTime");
		
		parsedString = StringUtil.replace(parsedString, "[%TARGET_GO_LIVE_DATE]", targetGoLiveDateTime.split("T")[0]);

		String reason = businessEventPropertiesJSONObject.optString("lastComment", "");

		if (StringUtil.equalsIgnoreCase(reason, "")){
			parsedString = StringUtil.replace(parsedString, "[%REASON_IF_ANY]", "");
		}
		else {
			parsedString = StringUtil.replace(parsedString, "[%REASON_IF_ANY]", "<p>" + reason + "</p>");
		}

		
		return parsedString;
	}

	private void _sendNotification(JSONObject businessEventJSONObject, JSONObject businessEventPropertiesJSONObject, JSONObject jsonObject, JSONObject koroneikiAccountJSONObject, Jwt jwt) throws Exception {
		String notificationTemplateERC = _getNotificationTemplateERC(jsonObject, businessEventPropertiesJSONObject);

		JSONObject notificationTemplateJSONObject = _getNotificationTemplateJSONObject(notificationTemplateERC, jwt);

		if (notificationTemplateJSONObject.isEmpty()) {
			throw new Exception("No template found for external reference code " + notificationTemplateERC);
		}

		JSONObject payloadJSONObject = _getPayloadJSONObject(businessEventJSONObject, businessEventPropertiesJSONObject, koroneikiAccountJSONObject, notificationTemplateJSONObject, jwt);

		post(
				"Bearer " + jwt.getTokenValue(),
				payloadJSONObject.toString(),
				"/o/notification/v1.0/notification-queue-entries");
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionBusinessEventRestController.class);

}