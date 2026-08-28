/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Franca
 */
@Component
public class EnvironmentAdminService extends OneBaseService {

	public void addEnvironmentAdmins(
			JSONArray adminsJSONArray, long environmentId)
		throws Exception {

		for (int i = 0; i < adminsJSONArray.length(); i++) {
			JSONObject adminJSONObject = adminsJSONArray.getJSONObject(i);

			post(
				getAuthorization(),
				new JSONObject(
				).put(
					"emailAddress", adminJSONObject.optString("emailAddress")
				).put(
					"firstName", adminJSONObject.optString("firstName")
				).put(
					"githubUsername",
					adminJSONObject.optString("githubUsername")
				).put(
					"lastName", adminJSONObject.optString("lastName")
				).put(
					"r_environmentToEnvironmentAdmin_c_environmentId",
					environmentId
				).toString(),
				UriComponentsBuilder.fromPath(
					"/o/c/environmentadmins"
				).build(
				).toUri());
		}
	}

}