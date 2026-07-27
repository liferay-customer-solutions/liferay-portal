/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.Validator;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class Property {

	public Property(JSONObject jsonObject) {
		_accountEntryId = jsonObject.optLong(
			"r_accountEntryToProperty_accountEntryId");
		_className = jsonObject.optString("className");
		_classNameId = jsonObject.optLong("classNameId");
		_classPK = jsonObject.optLong("classPK");
		_externalReferenceCode = jsonObject.optString("externalReferenceCode");

		String metadataJSON = jsonObject.optString("metadataJson");

		if (Validator.isNotNull(metadataJSON)) {
			_metadataJSONObject = new JSONObject(metadataJSON);
		}
		else {
			_metadataJSONObject = new JSONObject();
		}

		_name = jsonObject.optString("name");
		_propertyId = jsonObject.getLong("id");
		_value = jsonObject.optString("value");
	}

	public long getAccountEntryId() {
		return _accountEntryId;
	}

	public String getClassName() {
		return _className;
	}

	public long getClassNameId() {
		return _classNameId;
	}

	public long getClassPK() {
		return _classPK;
	}

	public String getExternalReferenceCode() {
		return _externalReferenceCode;
	}

	public JSONObject getMetadataJSONObject() {
		return _metadataJSONObject;
	}

	public String getName() {
		return _name;
	}

	public long getPropertyId() {
		return _propertyId;
	}

	public String getValue() {
		return _value;
	}

	public JSONObject toExternalLinkJSONObject() {
		int index = _name.indexOf(CharPool.COLON);

		String domain = _name;

		String entityName = StringPool.BLANK;

		if (index >= 0) {
			domain = _name.substring(0, index);
			entityName = _name.substring(index + 1);
		}

		return new JSONObject(
		).put(
			"domain", domain
		).put(
			"entityId", _value
		).put(
			"entityName", entityName
		);
	}

	private final long _accountEntryId;
	private final String _className;
	private final long _classNameId;
	private final long _classPK;
	private final String _externalReferenceCode;
	private final JSONObject _metadataJSONObject;
	private final String _name;
	private final long _propertyId;
	private final String _value;

}