/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Franca
 */
public class AssetObject {

	public AssetObject(JSONObject assetObjectJSONObject) {
		_id = assetObjectJSONObject.getString("id");
		_name = assetObjectJSONObject.getString("name");
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			"id", _id
		).put(
			"name", _name
		);
	}

	private final String _id;
	private final String _name;

}