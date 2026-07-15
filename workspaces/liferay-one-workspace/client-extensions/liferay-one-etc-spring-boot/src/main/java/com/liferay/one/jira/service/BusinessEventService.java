/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.service;

import com.liferay.one.jira.constants.BusinessEventConstants;
import com.liferay.one.jira.converter.BusinessEventConverter;
import com.liferay.one.jira.converter.BusinessEventVersionConverter;
import com.liferay.one.jira.model.AssetObjectFieldOption;
import com.liferay.one.jira.model.BusinessEvent;
import com.liferay.one.jira.model.BusinessEventVersion;
import com.liferay.one.jira.model.ProductVersion;
import com.liferay.one.jira.util.AQLUtil;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Jenny Chen
 * @author Drew Brokke
 */
@Component
public class BusinessEventService {

	public void createBusinessEvent(BusinessEvent businessEvent)
		throws Exception {

		_jiraAssetService.createObject(
			_businessEventConverter.getObjectTypeId(),
			_businessEventConverter.toAssetObject(
				_accountAssetService.getAccountObjectKey(
					businessEvent.getProjectExternalReferenceCode()),
				businessEvent));
	}

	public void deleteBusinessEvent(String id) throws Exception {
		_jiraAssetService.deleteObject(id);
	}

	public BusinessEvent getBusinessEvent(String id) throws Exception {
		return _businessEventConverter.toBusinessEvent(
			_jiraAssetService.getObject(id), StringPool.BLANK);
	}

	public List<BusinessEvent> getBusinessEvents(
			String projectExternalReferenceCode)
		throws Exception {

		return _jiraAssetService.searchObjects(
			_businessEventConverter.getAQLWithBuilder(
				aqlBuilder -> aqlBuilder.andEquals(
					projectExternalReferenceCode,
					BusinessEventConstants.ATTRIBUTE_NAME_ACCOUNT,
					"External Key")),
			jsonObject -> _businessEventConverter.toBusinessEvent(
				jsonObject, projectExternalReferenceCode));
	}

	public List<BusinessEventVersion> getBusinessEventVersions(
			String businessEventId)
		throws Exception {

		if (!Validator.isNumber(businessEventId)) {
			return new ArrayList<>();
		}

		return _jiraAssetService.searchObjects(
			_businessEventVersionConverter.getAQLWithBuilder(
				aqlBuilder -> aqlBuilder.andEqualsObject(
					businessEventId,
					BusinessEventConstants.OBJECT_TYPE_BUSINESS_EVENT
				).orderByDescending(
					"Updated"
				)),
			_businessEventVersionConverter::toBusinessEventVersion);
	}

	@Cacheable("assetObjectFieldOptions")
	public List<AssetObjectFieldOption> getFieldOptions(String fieldName)
		throws Exception {

		List<AssetObjectFieldOption> assetObjectFieldOptions =
			new ArrayList<>();

		JSONArray objectTypeAttributesJSONArray =
			_jiraAssetService.getObjectTypeAttributes(
				_businessEventConverter.getObjectTypeId());

		for (int i = 0; i < objectTypeAttributesJSONArray.length(); i++) {
			JSONObject objectTypeAttributeJSONObject =
				objectTypeAttributesJSONArray.getJSONObject(i);

			if (!fieldName.equals(
					objectTypeAttributeJSONObject.optString("name"))) {

				continue;
			}

			String options = objectTypeAttributeJSONObject.optString("options");

			if (Validator.isNotNull(options)) {
				for (String option : options.split(",")) {
					option = option.trim();

					assetObjectFieldOptions.add(
						new AssetObjectFieldOption(option, option));
				}
			}

			break;
		}

		return assetObjectFieldOptions;
	}

	@Cacheable("productVersions")
	public List<ProductVersion> getProductVersions() throws Exception {
		return _jiraAssetService.searchObjects(
			AQLUtil.getBaseAQL(
				BusinessEventConstants.OBJECT_SCHEMA_BUSINESS_EVENTS,
				BusinessEventConstants.OBJECT_TYPE_PRODUCT_VERSION),
			ProductVersion::new);
	}

	@CacheEvict(
		allEntries = true,
		value = {
			"assetObjectFieldOptions", "assetObjectTypeAttributeIds",
			"assetObjectTypeAttributeOptions", "assetObjectTypeIds",
			"productVersions"
		}
	)
	@Scheduled(cron = "0 0 0 * * *")
	public void scheduledAssetObjectsCacheEviction() throws Exception {
	}

	public BusinessEvent updateBusinessEvent(
			BusinessEvent businessEvent, String id)
		throws Exception {

		_jiraAssetService.updateObject(
			id, _businessEventConverter.toAssetObject(null, businessEvent));

		return getBusinessEvent(id);
	}

	@Autowired
	private AccountAssetService _accountAssetService;

	@Autowired
	private BusinessEventConverter _businessEventConverter;

	@Autowired
	private BusinessEventVersionConverter _businessEventVersionConverter;

	@Autowired
	private JiraAssetService _jiraAssetService;

}