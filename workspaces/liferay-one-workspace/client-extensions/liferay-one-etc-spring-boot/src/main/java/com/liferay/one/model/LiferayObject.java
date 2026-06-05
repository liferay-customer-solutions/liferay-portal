/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.portal.kernel.util.GetterUtil;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Amos Fong
 */
public class LiferayObject {

	public LiferayObject(JSONObject jsonObject) {
		JSONArray customFieldsJSONArray = jsonObject.optJSONArray(
			"customFields");

		if (customFieldsJSONArray == null) {
			return;
		}

		for (int i = 0; i < customFieldsJSONArray.length(); i++) {
			JSONObject customFieldJSONObject =
				customFieldsJSONArray.getJSONObject(i);

			JSONObject customValueJSONObject =
				customFieldJSONObject.optJSONObject("customValue");

			if (customValueJSONObject == null) {
				continue;
			}

			_customFieldValues.put(
				customFieldJSONObject.optString("name"),
				customValueJSONObject.opt("data"));
		}
	}

	protected boolean getCustomFieldBoolean(String name) {
		return GetterUtil.getBoolean(_customFieldValues.get(name));
	}

	protected double getCustomFieldDouble(String name) {
		return GetterUtil.getDouble(_customFieldValues.get(name));
	}

	protected int getCustomFieldInteger(String name) {
		return GetterUtil.getInteger(_customFieldValues.get(name));
	}

	protected long getCustomFieldLong(String name) {
		return GetterUtil.getLong(_customFieldValues.get(name));
	}

	protected String getCustomFieldString(String name) {
		return GetterUtil.getString(_customFieldValues.get(name));
	}

	private final Map<String, Object> _customFieldValues = new HashMap<>();

}
