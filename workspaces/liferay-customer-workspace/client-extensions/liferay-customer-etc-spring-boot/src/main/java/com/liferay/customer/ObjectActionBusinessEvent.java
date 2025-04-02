/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.petra.string.StringBundler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
public class ObjectActionBusinessEvent extends BaseRestController {

	@RequestMapping(
		method = RequestMethod.POST, path = "/object/action/business/event"
	)
	public ResponseEntity<String> post(
		@AuthenticationPrincipal Jwt jwt, @RequestBody String json) {

		JSONObject jsonObject = new JSONObject(json);

		JSONObject businessEventJSONObject = jsonObject.getJSONObject(
			"objectEntryDTOBusinessEvent");

		JSONObject businessEventPropertiesJSONObject =
			businessEventJSONObject.getJSONObject("properties");

		JSONObject businessEventVersionJSONObject = new JSONObject();

		JSONObject businessEventVersionChangeJSONObject = new JSONObject();

		String objectActionTriggerKey = jsonObject.getString(
			"objectActionTriggerKey");

		if (objectActionTriggerKey.equals("onAfterAdd")) {
			businessEventVersionChangeJSONObject.put(
				"key", "created"
			).put(
				"name", "Created"
			);

			businessEventVersionJSONObject.put(
				"change", businessEventVersionChangeJSONObject
			).put(
				"comment", "New Business Event has been created."
			);
		}
		else {
			String businesEventActualGoLiveDateTime =
				businessEventPropertiesJSONObject.optString(
					"actualGoLiveDateTime", null);

			if (businesEventActualGoLiveDateTime != null) {
				businessEventVersionChangeJSONObject.put(
					"key", "goLive"
				).put(
					"name", "Go-Live"
				);
			}
			else {
				businessEventVersionChangeJSONObject.put(
					"key", "edited"
				).put(
					"name", "Edited"
				);
			}

			businessEventVersionJSONObject.put(
				"change", businessEventVersionChangeJSONObject
			).put(
				"comment",
				businessEventPropertiesJSONObject.optString("lastComment")
			);
		}

		businessEventVersionJSONObject.put(
			"r_accountEntryToBusinessEventVersions_accountEntryId",
			businessEventPropertiesJSONObject.getString(
				"r_accountEntryToBusinessEvents_accountEntryId")
		).put(
			"r_businessEventToBusinessEventVersions_c_businessEventId",
			businessEventJSONObject.getString("id")
		);

		try {
			post(
				"Bearer " + jwt.getTokenValue(),
				businessEventVersionJSONObject.toString(),
				"/o/c/businesseventversions");

			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception exception) {
			StringBundler sb = new StringBundler(4);

			sb.append("Unable to create business event version:\n");
			sb.append(businessEventVersionJSONObject.toString());
			sb.append("\nAuthor's ID: ");
			sb.append(jwt.getClaimAsString("sub"));

			_log.error(sb.toString(), exception);

			return new ResponseEntity(
				exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionBusinessEvent.class);

}