/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.converter;

import com.liferay.one.jira.model.JiraAssetObject;
import com.liferay.one.jira.service.AssetSchemaService;
import com.liferay.one.jira.util.AQLUtil;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Amos Fong
 * @author Drew Brokke
 */
public abstract class BaseAssetObjectConverter {

	public JiraAssetObject createJiraAssetObject() {
		return new JiraAssetObject(_getAttributeIds(), _getAttributeOptions());
	}

	public String getAQLWithBuilder(Consumer<AQLUtil.Builder> consumer) {
		AQLUtil.Builder builder = AQLUtil.builder(getBaseAQL());

		if (consumer != null) {
			consumer.accept(builder);
		}

		return builder.build();
	}

	public String getExternalKeyAttributeName() {
		return _ATTRIBUTE_NAME_EXTERNAL_KEY;
	}

	public String getObjectTypeId() {
		return _assetSchemaService.getObjectTypeId(
			getObjectSchemaName(), getObjectTypeName());
	}

	public abstract String getObjectTypeName();

	public JiraAssetObject toJiraAssetObject(JSONObject jsonObject) {
		return new JiraAssetObject(
			jsonObject, _getAttributeIds(), _getAttributeOptions());
	}

	protected String formatDate(Date date) {
		if (date == null) {
			return null;
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ssXX");

		simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		return simpleDateFormat.format(date);
	}

	protected String getBaseAQL() {
		return AQLUtil.getBaseAQL(getObjectSchemaName(), getObjectTypeName());
	}

	protected abstract String getObjectSchemaName();

	private Map<String, String> _getAttributeIds() {
		return _assetSchemaService.getAttributeIds(
			getObjectSchemaName(), getObjectTypeName());
	}

	private Map<String, Set<String>> _getAttributeOptions() {
		return _assetSchemaService.getAttributeOptions(
			getObjectSchemaName(), getObjectTypeName());
	}

	private static final String _ATTRIBUTE_NAME_EXTERNAL_KEY = "External Key";

	@Autowired
	private AssetSchemaService _assetSchemaService;

}