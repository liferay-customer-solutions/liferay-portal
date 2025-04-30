/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.customer.constants.NotificationTemplateERCConstants;
import com.liferay.customer.permission.BusinessEventPermission;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.security.permission.ActionKeys;
import com.liferay.portal.kernel.util.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

	@RequestMapping(
		method = RequestMethod.POST, path = "/object/action/business/event"
	)
	public ResponseEntity<String> post(
			@AuthenticationPrincipal Jwt jwt,
			@RequestBody String json) {

		try {
			JSONObject jsonObject = new JSONObject(json);

			JSONObject businessEventJSONObject = jsonObject.getJSONObject(
				"objectEntryDTOBusinessEvent");

			JSONObject propertiesJSONObject =
				businessEventJSONObject.getJSONObject("properties");

			_businessEventPermission.check(
					jwt, propertiesJSONObject.getString("accountEntryToBusinessEventsERC"), ActionKeys.UPDATE);
			
			_createBusinessEventVersion(jsonObject);

			_sendNotification(jsonObject);
		}
		catch (Exception exception) {

			_log.error(exception, exception);

			return new ResponseEntity(
				exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void _createBusinessEventVersion(JSONObject jsonObject) throws Exception {
		String action = _getAction(jsonObject);

		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
			"objectEntryDTOBusinessEvent");

		JSONObject propertiesJSONObject =
			businessEventJSONObject.getJSONObject("properties");
			
		JSONObject businessEventVersionJSONObject = new JSONObject(
			).put(
				"change",
				_getChangeJSONObject(action, propertiesJSONObject)
			).put(
				"comment",
				_getComment(action, propertiesJSONObject)
			).put(
				"r_accountEntryToBusinessEventVersions_accountEntryId",
				propertiesJSONObject.getString(
					"r_accountEntryToBusinessEvents_accountEntryId")
			).put(
				"r_businessEventToBusinessEventVersions_c_businessEventId",
				businessEventJSONObject.getString("id")
			);

		_postBusinessEventVersion(businessEventVersionJSONObject);
	}

	private String _getAction(JSONObject jsonObject) throws Exception {
		String action = jsonObject.getString("objectActionTriggerKey");

		if (!StringUtil.equals(action, "onAfterAdd") && !StringUtil.equals(action, "onAfterUpdate")) {
			throw new Exception("Invalid action: " + action);
		}

		return action;
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private JSONObject _getChangeJSONObject(
		String action, JSONObject propertiesJSONObject) {

		if (StringUtil.equals(action, "onAfterAdd")) {
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
		String action, JSONObject propertiesJSONObject) {

		if (StringUtil.equals(action, "onAfterAdd")) {
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

	private JSONObject _getKoroneikiAccountJSONObject(String externalReferenceCode)
		throws Exception {

		JSONObject koroneikyAccountJSONObject = new JSONObject(
			get(
				_getAuthorization(),
				"/o/c/koroneikiaccounts/by-external-reference-code/" +
					externalReferenceCode));

		if (koroneikyAccountJSONObject.isEmpty()) {
			throw new Exception("No koroneiki account found for external reference code " + externalReferenceCode);
		}

		return koroneikyAccountJSONObject;
	}

	private String _getNotificationTemplateERC(
		String action, JSONObject propertiesJSONObject) {

		JSONObject changeJSONObject = _getChangeJSONObject(
			action, propertiesJSONObject);

		String changeKey = changeJSONObject.getString("key");

		if (StringUtil.equals(changeKey, "created")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_CREATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equals(changeKey, "edited")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_UPDATED_NOTIFICATION_TEMPLATE;
		}

		if (StringUtil.equals(changeKey, "eventCanceled")) {
			return NotificationTemplateERCConstants.
				BUSINESS_EVENT_CANCELED_NOTIFICATION_TEMPLATE;
		}

		return NotificationTemplateERCConstants.
			BUSINESS_EVENT_COMPLETED_NOTIFICATION_TEMPLATE;
	}

	private JSONObject _getNotificationTemplateJSONObject(String externalReferenceCode) throws Exception {
		JSONObject notificationTemplateJSONObject = new JSONObject(
			get(
				_getAuthorization(),
				"/o/notification/v1.0/notification-templates/by-external-reference-code/" +
					externalReferenceCode));

		if (notificationTemplateJSONObject.isEmpty()) {
			throw new Exception("No notification template found for external reference code " + externalReferenceCode);
		}

		return notificationTemplateJSONObject;
	}

	private JSONObject _getPayloadJSONObject(
			JSONObject businessEventJSONObject,
			JSONObject koroneikiAccountJSONObject,
			JSONObject notificationTemplateJSONObject,
			JSONObject propertiesJSONObject)
		throws Exception {

		JSONObject notificationTemplateBodyJSONObject =
			notificationTemplateJSONObject.getJSONObject("body");

		String notificationTemplateBody = _parseString(
			businessEventJSONObject, koroneikiAccountJSONObject,
			propertiesJSONObject, notificationTemplateBodyJSONObject.getString("en_US"));
		
		JSONArray notificationTemplateRecipientsJSONArray = _parseRecipientsJSONArray(
			koroneikiAccountJSONObject,
			notificationTemplateJSONObject.getJSONArray("recipients"));

		JSONObject notificationTemplateSubjectJSONObject =
			notificationTemplateJSONObject.getJSONObject("subject");

		String notificationTemplateSubject = _parseString(
			businessEventJSONObject, koroneikiAccountJSONObject,
			propertiesJSONObject, notificationTemplateSubjectJSONObject.getString("en_US"));

		return new JSONObject(
		).put(
			"body", notificationTemplateBody
		).put(
			"recipients", notificationTemplateRecipientsJSONArray
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

	private boolean _hasTAMServiceSubscription(String externalReferenceCode) throws Exception {
		JSONObject accountSubscriptionsJSONObject = new JSONObject(
			get(_getAuthorization(), "/o/c/accountsubscriptions/?filter=(name eq 'Technical Account Management Services' or name eq 'Technical Account Management Services - LATAM') and accountKey eq '" +
				externalReferenceCode + "'"));

		JSONArray accountSubscriptionsJSONArray =
			accountSubscriptionsJSONObject.getJSONArray("items");

		if (accountSubscriptionsJSONArray.length() > 0) {
			return true;
		}

		return false;
	}

	private boolean _isCanceledEvent(JSONObject propertiesJSONObject) {
		JSONObject eventStatusJSONObject = propertiesJSONObject.getJSONObject(
			"eventStatus");

		return StringUtil.equals(
			eventStatusJSONObject.getString("key"), "canceled");
	}

	private boolean _isGoLive(JSONObject propertiesJSONObject) {
		return !StringUtil.equals(
			propertiesJSONObject.optString("actualGoLiveDateTime"), "");
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
		JSONObject koroneikiAccountJSONObject, 
		JSONObject propertiesJSONObject, String string) {

		StringBundler sb = new StringBundler(2);

		sb.append("https://support.liferay.com/project/#/");
		sb.append(
			propertiesJSONObject.getString(
				"accountEntryToBusinessEventsERC") + "/business-events/" +
					businessEventJSONObject.getString("id"));

		String parsedString;

		parsedString = StringUtil.replace(
			string, "[%BUSINESS_EVENT_LINK]", sb.toString());

		parsedString = StringUtil.replace(
			parsedString, "[%EVENT_NAME]",
			propertiesJSONObject.getString("name"));

		JSONObject eventTypeJSONObject =
		propertiesJSONObject.getJSONObject("eventType");

		parsedString = StringUtil.replace(
			parsedString, "[%EVENT_TYPE]",
			eventTypeJSONObject.getString("name"));

		parsedString = StringUtil.replace(
			parsedString, "[%PROJECT_NAME]",
			koroneikiAccountJSONObject.getString("name"));

		String feedback = propertiesJSONObject.optString(
			"feedback");

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
		propertiesJSONObject.getString("targetGoLiveDateTime");

		parsedString = StringUtil.replace(
			parsedString, "[%TARGET_GO_LIVE_DATE]",
			targetGoLiveDateTime.split("T")[0]);

		String reason = propertiesJSONObject.optString(
			"lastComment");

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

	private void _postBusinessEventVersion (JSONObject businessEventVersionJSONObject) throws Exception {
		try {
			post(
				_getAuthorization(), businessEventVersionJSONObject.toString(),
				"/o/c/businesseventversions");
		} catch (Exception exception) {
			StringBundler sb = new StringBundler(2);

			sb.append("Unable to create business event version:\n");
			sb.append(businessEventVersionJSONObject.toString());

			throw new Exception(sb.toString(), exception);
		}
	}

	private void _sendNotification(JSONObject jsonObject) throws Exception {
		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
			"objectEntryDTOBusinessEvent");
		
		JSONObject propertiesJSONObject =
			businessEventJSONObject.getJSONObject("properties");
		
		JSONObject koroneikiAccountJSONObject = _getKoroneikiAccountJSONObject(
				propertiesJSONObject.getString(
					"accountEntryToBusinessEventsERC"));
		
		String action = _getAction(jsonObject);
		
		String notificationTemplateERC = _getNotificationTemplateERC(
			action, propertiesJSONObject);

		JSONObject notificationTemplateJSONObject =
			_getNotificationTemplateJSONObject(notificationTemplateERC);

		JSONObject payloadJSONObject = _getPayloadJSONObject(
			businessEventJSONObject, koroneikiAccountJSONObject, notificationTemplateJSONObject, propertiesJSONObject);

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

	@Autowired
	private BusinessEventPermission _businessEventPermission;

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