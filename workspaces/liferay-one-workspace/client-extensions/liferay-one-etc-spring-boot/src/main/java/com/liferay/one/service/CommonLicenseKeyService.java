/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.constants.UploadProductEnvironmentConstants;
import com.liferay.one.constants.UploadProductGroupConstants;
import com.liferay.one.license.CommonLicenseKeyData;
import com.liferay.one.license.CommonLicenseKeyParser;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.List;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Allen Ziegenfus
 */
@Component
public class CommonLicenseKeyService extends OneBaseService {

	public void addCommonLicenseKey(
			String fileContent, String fileName, long fileSize,
			String productGroup)
		throws Exception {

		CommonLicenseKeyData commonLicenseKeyData = null;
		String productFamily = null;

		if (StringUtil.equals(
				productGroup, UploadProductGroupConstants.ENTERPRISE_SEARCH)) {

			commonLicenseKeyData =
				_commonLicenseKeyParser.parseEnterpriseSearch(fileContent);
			productFamily = _PRODUCT_FAMILY_ENTERPRISE_SEARCH;
		}
		else {
			commonLicenseKeyData = _commonLicenseKeyParser.parseCommerce(
				fileContent);
			productFamily = _PRODUCT_FAMILY_COMMERCE;
		}

		JSONObject jsonObject = new JSONObject(
		).put(
			"endDate",
			commonLicenseKeyData.getEndDateInstant(
			).toString()
		).put(
			"environmentType",
			new JSONObject(
			).put(
				"key",
				_toEnvironmentTypeKey(
					commonLicenseKeyData.getProductEnvironment())
			)
		).put(
			"fileContent", fileContent
		).put(
			"fileName", fileName
		).put(
			"fileSize", fileSize
		).put(
			"productFamily",
			new JSONObject(
			).put(
				"key", productFamily
			)
		).put(
			"startDate",
			commonLicenseKeyData.getStartDateInstant(
			).toString()
		);

		postCommonLicenseKey(jsonObject);
	}

	public JSONObject getCommonLicenseKey(
			String environmentType, String productFamily)
		throws Exception {

		List<JSONObject> commonLicenseKeys = getAllItems(
			"/o/c/commonlicensekeys",
			StringBundler.concat(
				"(environmentType eq '", environmentType,
				"') and (productFamily eq '", productFamily, "')"),
			jsonObject -> jsonObject);

		if (commonLicenseKeys.isEmpty()) {
			return null;
		}

		return commonLicenseKeys.get(0);
	}

	public boolean hasCommonLicenseKey(String fileName) throws Exception {
		List<JSONObject> commonLicenseKeys = _getCommonLicenseKeys(fileName);

		return !commonLicenseKeys.isEmpty();
	}

	protected void postCommonLicenseKey(JSONObject jsonObject)
		throws Exception {

		post(
			getAuthorization(), jsonObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/commonlicensekeys"
			).build(
			).toUri());
	}

	private List<JSONObject> _getCommonLicenseKeys(String fileName)
		throws Exception {

		return getAllItems(
			"/o/c/commonlicensekeys",
			"fileName eq '" + StringUtil.replace(fileName, '\'', "''") + "'",
			jsonObject -> jsonObject);
	}

	private String _toEnvironmentTypeKey(String productEnvironment) {
		if (StringUtil.equals(
				productEnvironment,
				UploadProductEnvironmentConstants.NONPRODUCTION)) {

			return "nonProduction";
		}

		return productEnvironment;
	}

	private static final String _PRODUCT_FAMILY_COMMERCE = "commerce";

	private static final String _PRODUCT_FAMILY_ENTERPRISE_SEARCH =
		"enterpriseSearch";

	@Autowired
	private CommonLicenseKeyParser _commonLicenseKeyParser;

}