/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import com.liferay.one.constants.SupportLanguageConstants;
import com.liferay.portal.kernel.util.Validator;

import java.util.Objects;

/**
 * @author Drew Brokke
 */
public class SupportLanguageUtil {

	public static String getLanguage(String soldBy, String countryName) {
		if (Validator.isNull(soldBy)) {
			return SupportLanguageConstants.ENGLISH;
		}

		if (soldBy.equals("Liferay Africa") ||
			soldBy.equals("Liferay Australia") ||
			soldBy.equals("Liferay Canada") ||
			soldBy.equals("Liferay France") ||
			soldBy.equals("Liferay Germany") ||
			soldBy.equals("Liferay Hungary") ||
			soldBy.equals("Liferay India") ||
			soldBy.equals("Liferay International") ||
			soldBy.equals("Liferay Italy") ||
			soldBy.equals("Liferay Middle East") ||
			soldBy.equals("Liferay Netherlands") ||
			soldBy.equals("Liferay Nordic") ||
			soldBy.equals("Liferay Singapore") || soldBy.equals("Liferay UK") ||
			soldBy.equals("Liferay US")) {

			return SupportLanguageConstants.ENGLISH;
		}
		else if (soldBy.equals("Liferay Brazil")) {
			if (Objects.equals(countryName, "Brazil")) {
				return SupportLanguageConstants.PORTUGUESE;
			}

			return SupportLanguageConstants.SPANISH;
		}
		else if (soldBy.equals("Liferay China")) {
			if (Objects.equals(countryName, "China")) {
				return SupportLanguageConstants.CHINESE;
			}

			return SupportLanguageConstants.ENGLISH;
		}
		else if (soldBy.equals("Liferay Japan")) {
			return SupportLanguageConstants.JAPANESE;
		}
		else if (soldBy.equals("Liferay Spain")) {
			if (Objects.equals(countryName, "Cyprus") ||
				Objects.equals(countryName, "Greece") ||
				Objects.equals(countryName, "Italy") ||
				Objects.equals(countryName, "Portugal")) {

				return SupportLanguageConstants.ENGLISH;
			}

			return SupportLanguageConstants.SPANISH;
		}

		return SupportLanguageConstants.ENGLISH;
	}

}