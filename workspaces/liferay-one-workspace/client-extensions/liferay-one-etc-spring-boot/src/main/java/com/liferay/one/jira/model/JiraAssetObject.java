/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.model;

import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author Drew Brokke
 */
public class JiraAssetObject {

	public JiraAssetObject(
		JSONObject jsonObject, Map<String, String> attributeNameToIdsMap,
		Map<String, Set<String>> attributeNameToOptionsMap) {

		_jsonObject = jsonObject;

		_attributeIds = attributeNameToIdsMap;
		_attributeOptions = attributeNameToOptionsMap;
	}

	public JiraAssetObject(
		Map<String, String> attributeNameToIdsMap,
		Map<String, Set<String>> attributeNameToOptionsMap) {

		this(
			new JSONObject(), attributeNameToIdsMap, attributeNameToOptionsMap);
	}

	/**
	 * Attempts to get the named attribute's display value. Falls back to the
	 * result of {@link #getAttributeValue(String)} if no display value is
	 * found.
	 */
	public String getAttributeDisplayValue(String attributeName) {
		JSONObject attributeValueJSONObject = _getAttributeValueJSONObject(
			attributeName);

		return attributeValueJSONObject.optString(
			"displayValue", _getValue(attributeValueJSONObject));
	}

	/**
	 * Gets the named attribute's value. If not found, will attempt to
	 * return the id of the referenced object.
	 */
	public String getAttributeValue(String attributeName) {
		String attributeId = _getAttributeId(attributeName);

		if (_values.containsKey(attributeId)) {
			return String.valueOf(_values.get(attributeId));
		}

		return _getValue(_getAttributeValueJSONObject(attributeName));
	}

	/**
	 * The object's own id (top-level {@code id}), not an attribute value.
	 */
	public String getObjectId() {
		return _jsonObject.getString("id");
	}

	/**
	 * The object's own key (top-level {@code objectKey}), not an attribute
	 * value.
	 */
	public String getObjectKey() {
		return _jsonObject.getString("objectKey");
	}

	/**
	 * The object's own name (top-level {@code name}), not an attribute value.
	 */
	public String getObjectName() {
		return _jsonObject.getString("name");
	}

	public void setAttributeValue(String attributeName, Object value) {
		String attributeId = _getAttributeId(attributeName);

		if ((attributeId == null) || (value == null)) {
			return;
		}

		if (value instanceof Collection<?> collection) {
			for (Object object : collection) {
				_checkOption(attributeName, object);
			}
		}
		else {
			_checkOption(attributeName, value);
		}

		_values.put(attributeId, value);
	}

	/**
	 * Serializes this object into the JSM's {@code attributes} JSON shape.
	 * This method is used by the {@link com.liferay.one.jira.service.JiraAssetService}.
	 */
	public JSONArray toAttributesJSONArray() {
		JSONArray attributesJSONArray = new JSONArray();

		for (Map.Entry<String, Object> entry : _values.entrySet()) {
			JSONArray objectAttributeValuesJSONArray = new JSONArray();

			Object value = entry.getValue();

			if (value instanceof Collection<?> collection) {
				for (Object object : collection) {
					if (object == null) {
						continue;
					}

					objectAttributeValuesJSONArray.put(
						_toAttrbuteValueJSONObject(object));
				}
			}
			else {
				objectAttributeValuesJSONArray.put(
					_toAttrbuteValueJSONObject(value));
			}

			attributesJSONArray.put(
				new JSONObject(
				).put(
					"objectAttributeValues", objectAttributeValuesJSONArray
				).put(
					"objectTypeAttributeId", entry.getKey()
				));
		}

		return attributesJSONArray;
	}

	private void _checkOption(String attributeName, Object value) {
		Set<String> options = _attributeOptions.get(attributeName);

		if ((options == null) || (value == null) ||
			options.contains(String.valueOf(value))) {

			return;
		}

		if (_log.isWarnEnabled()) {
			_log.warn(
				StringBundler.concat(
					"Value \"", value, "\" for attribute \"", attributeName,
					"\" does not match any of the schema's options ", options,
					"; the option list has likely drifted"));
		}
	}

	private String _getAttributeId(String attributeName) {
		String attributeId = _attributeIds.get(attributeName);

		if (Validator.isNull(attributeId)) {
			_log.error(
				"No attribute id is mapped for attribute name \"" +
					attributeName +
						"\"; a constant likely no longer matches the schema");

			return null;
		}

		return attributeId;
	}

	private JSONObject _getAttributeValueJSONObject(String attributeName) {
		String attributeId = _getAttributeId(attributeName);

		if (attributeId == null) {
			return new JSONObject();
		}

		JSONArray attributesJSONArray = _jsonObject.optJSONArray("attributes");

		if (attributesJSONArray == null) {
			return new JSONObject();
		}

		for (int i = 0; i < attributesJSONArray.length(); i++) {
			JSONObject attributeJSONObject = attributesJSONArray.getJSONObject(
				i);

			if (!attributeId.equals(
					attributeJSONObject.optString("objectTypeAttributeId"))) {

				continue;
			}

			JSONArray objectAttributeValuesJSONArray =
				attributeJSONObject.optJSONArray("objectAttributeValues");

			if ((objectAttributeValuesJSONArray == null) ||
				objectAttributeValuesJSONArray.isEmpty()) {

				break;
			}

			return objectAttributeValuesJSONArray.getJSONObject(0);
		}

		return new JSONObject();
	}

	private String _getValue(JSONObject attributeValueJSONObject) {
		String value = attributeValueJSONObject.optString("value");

		if (Validator.isNull(value)) {
			JSONObject referencedObjectJSONObject =
				attributeValueJSONObject.optJSONObject("referencedObject");

			if (referencedObjectJSONObject != null) {
				value = referencedObjectJSONObject.optString("id");
			}
		}

		return value;
	}

	private JSONObject _toAttrbuteValueJSONObject(Object attributeValue) {
		JSONObject jsonObject = new JSONObject();

		jsonObject.put("value", attributeValue);

		return jsonObject;
	}

	private static final Log _log = LogFactory.getLog(JiraAssetObject.class);

	private final Map<String, String> _attributeIds;
	private final Map<String, Set<String>> _attributeOptions;
	private final JSONObject _jsonObject;
	private final Map<String, Object> _values = new LinkedHashMap<>();

}