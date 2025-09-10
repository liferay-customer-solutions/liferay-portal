/**
 * SPDX-FileCopyrightText: (c) 2025 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.customer.exception.JiraIssueClosedException;
import com.liferay.customer.exception.JiraIssueNotFoundException;
import com.liferay.customer.exception.JiraOrganizationNotFoundException;
import com.liferay.customer.model.JiraSupportIssue;
import com.liferay.customer.service.JiraService;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringUtil;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Amos Fong
 */
public class BaseRestController
	extends com.liferay.client.extension.util.spring.boot3.BaseRestController {

	protected String getAccountKey(String jiraIssueKey) throws Exception {
		try {
			return _getAccountKey(jiraIssueKey);
		}
		catch (JiraOrganizationNotFoundException
					jiraOrganizationNotFoundException) {

			_log.error(
				jiraOrganizationNotFoundException,
				jiraOrganizationNotFoundException);

			throw new JiraIssueNotFoundException();
		}
	}

	private String _getAccountKey(String jiraIssueKey) throws Exception {
		JSONObject jsonObject = _jiraService.getIssueJSONObject(jiraIssueKey);

		if (jsonObject == null) {
			throw new JiraIssueNotFoundException();
		}

		JiraSupportIssue jiraSupportIssue = new JiraSupportIssue(jsonObject);

		if (jiraSupportIssue.isClosed()) {
			throw new JiraIssueClosedException();
		}

		try {
			List<String> organizationCompositeIdArray = StringUtil.split(
				jiraSupportIssue.getOrganization(), CharPool.COLON);

			JSONObject assetObjectJSONObject = _jiraService.getAssetObject(
				organizationCompositeIdArray.get(0),
				organizationCompositeIdArray.get(1));

			JSONArray jsonArray = assetObjectJSONObject.getJSONArray(
				"attributes");

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject attributeJSONObject = jsonArray.getJSONObject(i);

				JSONObject objectTypeAttributeJSONObject =
					attributeJSONObject.getJSONObject("objectTypeAttribute");

				String name = objectTypeAttributeJSONObject.getString("name");

				if (!name.equals("External Key")) {
					continue;
				}

				JSONArray objectAttributeValuesJSONArray =
					attributeJSONObject.getJSONArray("objectAttributeValues");

				JSONObject objectAttributeValuesJSONObject =
					objectAttributeValuesJSONArray.getJSONObject(0);

				return objectAttributeValuesJSONObject.getString("value");
			}
		}
		catch (Exception exception) {
			throw new JiraOrganizationNotFoundException(exception);
		}

		throw new JiraOrganizationNotFoundException();
	}

	private static final Log _log = LogFactory.getLog(
		TicketAttachmentsInitiateUploadRestController.class);

	@Autowired
	private JiraService _jiraService;

}