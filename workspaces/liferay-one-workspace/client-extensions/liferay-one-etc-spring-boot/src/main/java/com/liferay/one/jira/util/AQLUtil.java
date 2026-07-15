/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.jira.util;

import com.liferay.petra.function.transform.TransformUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collection;

/**
 * @author Drew Brokke
 */
public class AQLUtil {

	public static Builder builder(String baseAQL) {
		return new Builder(baseAQL);
	}

	public static String getBaseAQL(String objectSchema, String objectType) {
		return StringBundler.concat(
			"objectSchema = ", _quote(objectSchema), " AND objectType = ",
			_quote(objectType));
	}

	public static class Builder {

		public Builder andEquals(String value, String... fieldNames) {
			_sb.append(" AND ");
			_sb.append(_field(fieldNames));
			_sb.append(" = ");
			_sb.append(_quote(value));

			return this;
		}

		public Builder andEqualsObject(String objectId, String... fieldNames) {
			_sb.append(" AND ");
			_sb.append(_field(fieldNames));
			_sb.append(" = ");
			_sb.append(objectId);

			return this;
		}

		public Builder andIn(Collection<String> values, String... fieldNames) {
			if (values.isEmpty()) {
				throw new IllegalArgumentException(
					"An IN clause requires at least one value");
			}

			_sb.append(" AND ");
			_sb.append(_field(fieldNames));
			_sb.append(" IN (");
			_sb.append(
				StringUtil.merge(
					TransformUtil.transform(values, AQLUtil::_quote),
					StringPool.COMMA_AND_SPACE));
			_sb.append(")");

			return this;
		}

		public String build() {
			return _sb.toString();
		}

		public Builder orderByDescending(String fieldName) {
			_sb.append(" ORDER BY ");
			_sb.append(fieldName);
			_sb.append(" DESC");

			return this;
		}

		private Builder(String baseAQL) {
			_sb.append(baseAQL);
		}

		private final StringBundler _sb = new StringBundler();

	}

	private static String _field(String... fieldNames) {
		String[] quotedFieldNames = new String[fieldNames.length];

		for (int i = 0; i < fieldNames.length; i++) {
			quotedFieldNames[i] = _quote(fieldNames[i]);
		}

		return StringUtil.merge(quotedFieldNames, StringPool.PERIOD);
	}

	private static String _quote(String value) {
		String escapedValue = StringUtil.replace(
			value, new String[] {StringPool.BACK_SLASH, StringPool.QUOTE},
			new String[] {
				StringPool.DOUBLE_BACK_SLASH,
				StringPool.BACK_SLASH + StringPool.QUOTE
			});

		return StringPool.QUOTE + escapedValue + StringPool.QUOTE;
	}

}