/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.customer.exception.FileServerUnavailableException;
import com.liferay.customer.service.GoogleCloudStorageService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Karoline Silva
 */
@RestController
public class ObjectActionTicketAttachmentsRestController
	extends BaseRestController {

	@RequestMapping(
		method = RequestMethod.POST, path = "/object/action/ticket/attachment"
	)
	public ResponseEntity<String> post(@RequestBody String json) {
		try {
			JSONObject jsonObject = new JSONObject(json);

			_getObjectActionTriggerKey(jsonObject);

			_deleteGCSObject(_getTicketAttachmentJSONObject(jsonObject));
		}
		catch (IllegalArgumentException illegalArgumentException) {
			_log.error(illegalArgumentException, illegalArgumentException);

			return new ResponseEntity<>("", HttpStatus.OK);
		}
		catch (Exception exception) {
			_log.error(exception, exception);

			return new ResponseEntity(
				"UNEXPECTED_ERROR", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private String _buildGCSObjectName(
		String jiraIssueKey, long ticketAttachmentId, String fileName) {

		StringBundler sb = new StringBundler(6);

		sb.append("tickets/");
		sb.append(jiraIssueKey);
		sb.append("/");
		sb.append(ticketAttachmentId);
		sb.append("/");
		sb.append(fileName);

		return sb.toString();
	}

	private void _deleteGCSObject(JSONObject ticketAttachmentJSONObject)
		throws Exception {

		JSONObject propertiesJSONObject =
			ticketAttachmentJSONObject.getJSONObject("propertiesJSONObject");

		String fileName = propertiesJSONObject.optString("fileName");
		String gcsBucketName = propertiesJSONObject.optString("gcsBucketName");
		String jiraIssueKey = propertiesJSONObject.optString("jiraIssueKey");

		long ticketAttachmentId = ticketAttachmentJSONObject.getLong("id");

		String gcsObjectName = _buildGCSObjectName(
			jiraIssueKey, ticketAttachmentId, fileName);

		if (Validator.isBlank(gcsObjectName) || Validator.isBlank(fileName) ||
			Validator.isBlank(gcsBucketName)) {

			throw new Exception(
				"Could not construct valid GCS object from the payload for " +
					"TicketAttachment ID " + ticketAttachmentId);
		}

		try {
			_googleCloudStorageService.deleteObject(
				gcsBucketName, gcsObjectName);
		}
		catch (FileServerUnavailableException fileServerUnavailableException) {
			if (fileServerUnavailableException.isNotFoundException()) {
				_log.error(
					"GCS object was not found, likely already deleted: " +
						gcsObjectName);
			}
			else {
				throw fileServerUnavailableException;
			}
		}
	}

	private String _getObjectActionTriggerKey(JSONObject jsonObject)
		throws Exception {

		String objectActionTriggerKey = jsonObject.getString(
			"objectActionTriggerKey");

		if (!StringUtil.equals(objectActionTriggerKey, "onAfterDelete")) {
			throw new IllegalArgumentException(
				"Invalid trigger key: " + objectActionTriggerKey);
		}

		return objectActionTriggerKey;
	}

	private JSONObject _getTicketAttachmentJSONObject(JSONObject jsonObject) {
		return jsonObject.getJSONObject("objectEntryDTOTicketAttachment");
	}

	private static final Log _log = LogFactory.getLog(
		ObjectActionTicketAttachmentsRestController.class);

	@Autowired
	private GoogleCloudStorageService _googleCloudStorageService;

}