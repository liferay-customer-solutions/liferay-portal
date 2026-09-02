/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.UsageDefinition;
import com.liferay.portal.kernel.util.Validator;

import org.json.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Drew Brokke
 */
@Component
public class UsageDefinitionService extends OneBaseService {

	public UsageDefinition fetchUsageDefinition(String externalReferenceCode)
		throws Exception {

		String response = fetch(
			getAuthorization(),
			UriComponentsBuilder.fromPath(
				"/o/c/usagedefinitions/by-external-reference-code" +
					"/{externalReferenceCode}"
			).buildAndExpand(
				externalReferenceCode
			).toUri());

		if (Validator.isNull(response)) {
			return null;
		}

		return new UsageDefinition(new JSONObject(response));
	}

}