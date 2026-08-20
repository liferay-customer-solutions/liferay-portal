/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.catalog.client.dto.v1_0.Product;
import com.liferay.one.constants.EntitlementConstants;
import com.liferay.one.constants.ProductSpecificationConstants;
import com.liferay.one.constants.TaxonomyCategoryConstants;
import com.liferay.one.model.EntitlementDefinition;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Felipe Veloso
 */
@Component
public class EntitlementDefinitionService extends OneBaseService {

	public void generateEntitlementDefinition(long cProductId)
		throws Exception {

		List<String> categoryExternalReferenceCodes =
			_commerceProductService.getCategoryExternalReferenceCodes(
				cProductId);

		if (!categoryExternalReferenceCodes.contains(
				TaxonomyCategoryConstants.EXTERNAL_REFERENCE_CODE_APP) ||
			!ArrayUtil.contains(
				ProductSpecificationConstants.TYPES_LICENSE_KEY_GENERATING,
				_commerceProductService.getSpecificationValue(
					cProductId, ProductSpecificationConstants.KEY_TYPE))) {

			return;
		}

		Product product = _commerceProductService.fetchProduct(cProductId);

		if (product == null) {
			return;
		}

		String productName = _commerceProductService.getName(product);

		if (Validator.isNull(productName)) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to find the name of product " + cProductId);
			}

			return;
		}

		List<EntitlementDefinition> entitlementDefinitions =
			getEntitlementDefinitions(
				StringBundler.concat(
					"r_commerceProductToEntitlementDefinition_CProductId eq '",
					cProductId, "'"));

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			Map<String, String> definitionProductOptions =
				entitlementDefinition.getProductOptions();

			if (definitionProductOptions.isEmpty()) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Skipping product ", cProductId,
							": an unconditional entitlement definition ",
							"already exists"));
				}

				return;
			}
		}

		List<JSONObject> productOptionJSONObjects =
			_commerceProductService.getProductOptions(cProductId);

		if (productOptionJSONObjects.isEmpty()) {
			if (entitlementDefinitions.isEmpty()) {
				if (_log.isInfoEnabled()) {
					_log.info(
						StringBundler.concat(
							"Product ", cProductId,
							" has no product options, generating the ",
							"entitlement definition without product options"));
				}

				_addEntitlementDefinition(
					cProductId,
					StringBundler.concat(
						cProductId, StringPool.UNDERLINE,
						EntitlementConstants.NAME_LICENSE_GENERATION),
					productName, null);
			}

			return;
		}

		for (JSONObject productOptionJSONObject : productOptionJSONObjects) {
			JSONArray productOptionValuesJSONArray =
				productOptionJSONObject.optJSONArray("productOptionValues");

			if (productOptionValuesJSONArray == null) {
				continue;
			}

			for (int i = 0; i < productOptionValuesJSONArray.length(); i++) {
				JSONObject productOptionValueJSONObject =
					productOptionValuesJSONArray.getJSONObject(i);

				String optionKey = productOptionJSONObject.optString("key");
				String valueKey = productOptionValueJSONObject.optString("key");

				if (Validator.isNull(optionKey) || Validator.isNull(valueKey)) {
					continue;
				}

				String externalReferenceCode = StringBundler.concat(
					cProductId, StringPool.UNDERLINE, optionKey,
					StringPool.UNDERLINE, valueKey);

				if (_isCovered(entitlementDefinitions, optionKey, valueKey) ||
					_hasEntitlementDefinition(
						entitlementDefinitions, externalReferenceCode)) {

					continue;
				}

				try {
					_addEntitlementDefinition(
						cProductId, externalReferenceCode,
						StringBundler.concat(
							productName, StringPool.SPACE,
							_getProductOptionValueName(
								productOptionValueJSONObject)),
						String.valueOf(
							new JSONObject(
							).put(
								optionKey, valueKey
							)));
				}
				catch (Exception exception) {
					_log.error(
						StringBundler.concat(
							"Unable to create the entitlement definition for ",
							"product ", cProductId, " and option value ",
							valueKey),
						exception);
				}
			}
		}
	}

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString)
		throws Exception {

		return getAllItems(
			"/o/c/entitlementdefinitions", filterString,
			EntitlementDefinition::new);
	}

	public List<EntitlementDefinition> getEntitlementDefinitions(
			String filterString, Map<String, String> productOptions)
		throws Exception {

		List<EntitlementDefinition> entitlementDefinitions =
			getEntitlementDefinitions(filterString);

		Iterator<EntitlementDefinition> iterator =
			entitlementDefinitions.iterator();

		while (iterator.hasNext()) {
			EntitlementDefinition entitlementDefinition = iterator.next();

			if (!_matches(
					entitlementDefinition.getProductOptions(),
					productOptions)) {

				iterator.remove();
			}
		}

		return entitlementDefinitions;
	}

	private void _addEntitlementDefinition(
			long cProductId, String externalReferenceCode, String name,
			String productOptionsJSON)
		throws Exception {

		JSONObject entitlementDefinitionJSONObject = new JSONObject(
		).put(
			"active", true
		).put(
			"defaultQuantity", 1
		).put(
			"displayName", name
		).put(
			"name", name
		).put(
			"r_commerceProductToEntitlementDefinition_CProductId", cProductId
		);

		if (productOptionsJSON != null) {
			entitlementDefinitionJSONObject.put(
				"productOptions", productOptionsJSON);
		}

		put(
			getAuthorization(), entitlementDefinitionJSONObject.toString(),
			UriComponentsBuilder.fromPath(
				"/o/c/entitlementdefinitions/by-external-reference-code" +
					"/{externalReferenceCode}"
			).encode(
			).buildAndExpand(
				externalReferenceCode
			).toUri());

		if (_log.isInfoEnabled()) {
			_log.info(
				StringBundler.concat(
					"Generated the entitlement definition \"", name,
					"\" for product ", cProductId));
		}
	}

	private String _getProductOptionValueName(
		JSONObject productOptionValueJSONObject) {

		JSONObject nameJSONObject = productOptionValueJSONObject.optJSONObject(
			"name");

		if (nameJSONObject != null) {
			String localizedName = nameJSONObject.optString("en_US");

			if (Validator.isNotNull(localizedName)) {
				return localizedName;
			}
		}

		return productOptionValueJSONObject.optString("key");
	}

	private boolean _hasEntitlementDefinition(
		List<EntitlementDefinition> entitlementDefinitions,
		String externalReferenceCode) {

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			if (Objects.equals(
					externalReferenceCode,
					entitlementDefinition.getExternalReferenceCode())) {

				return true;
			}
		}

		return false;
	}

	private boolean _isCovered(
		List<EntitlementDefinition> entitlementDefinitions, String optionKey,
		String valueKey) {

		for (EntitlementDefinition entitlementDefinition :
				entitlementDefinitions) {

			Map<String, String> productOptions =
				entitlementDefinition.getProductOptions();

			if (valueKey.equals(productOptions.get(optionKey))) {
				return true;
			}
		}

		return false;
	}

	private boolean _matches(
		Map<String, String> entitlementDefinitionProductOptions,
		Map<String, String> productOptions) {

		for (Map.Entry<String, String> entry :
				entitlementDefinitionProductOptions.entrySet()) {

			String value = entry.getValue();

			if (!value.equals(productOptions.get(entry.getKey()))) {
				return false;
			}
		}

		return true;
	}

	private static final Log _log = LogFactory.getLog(
		EntitlementDefinitionService.class);

	@Autowired
	private CommerceProductService _commerceProductService;

}