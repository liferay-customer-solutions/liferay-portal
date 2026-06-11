/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Franca
 */
public class FieldOption {

	public FieldOption(String label, String value) {
		_label = label;
		_value = value;
	}

	public String getLabel() {
		return _label;
	}

	public String getValue() {
		return _value;
	}

	public JSONObject toJSONObject() {
		return new JSONObject(
		).put(
			"label", _label
		).put(
			"value", _value
		);
	}

	private final String _label;
	private final String _value;

}