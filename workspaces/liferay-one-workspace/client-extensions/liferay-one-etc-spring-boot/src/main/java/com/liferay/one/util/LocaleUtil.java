/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import com.liferay.portal.kernel.util.StringUtil;

import java.util.Locale;

/**
 * @author Felipe Veloso
 */
public class LocaleUtil {

	public static Locale fromLanguageId(String languageId) {
		return Locale.forLanguageTag(StringUtil.replace(languageId, '_', '-'));
	}

}