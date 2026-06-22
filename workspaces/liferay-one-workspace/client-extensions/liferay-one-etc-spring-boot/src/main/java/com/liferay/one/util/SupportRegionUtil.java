/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import com.liferay.one.constants.SupportRegionConstants;
import com.liferay.portal.kernel.util.Validator;

/**
 * @author Felipe Veloso
 */
public class SupportRegionUtil {

	public static String getSupportRegion(String soldBy, String countryName) {
		if (Validator.isNull(soldBy)) {
			return SupportRegionConstants.GLOBAL;
		}

		if (soldBy.equals("Liferay Africa") ||
			soldBy.equals("Liferay France") ||
			soldBy.equals("Liferay Germany") ||
			soldBy.equals("Liferay Hungary") ||
			soldBy.equals("Liferay International") ||
			soldBy.equals("Liferay Italy") ||
			soldBy.equals("Liferay Middle East") ||
			soldBy.equals("Liferay Netherlands") ||
			soldBy.equals("Liferay Nordic") || soldBy.equals("Liferay UK")) {

			return SupportRegionConstants.HUNGARY;
		}
		else if (soldBy.equals("Liferay Australia")) {
			return SupportRegionConstants.AUSTRALIA;
		}
		else if (soldBy.equals("Liferay Brazil")) {
			return SupportRegionConstants.BRAZIL;
		}
		else if (soldBy.equals("Liferay Canada") ||
				 soldBy.equals("Liferay US")) {

			return SupportRegionConstants.UNITED_STATES;
		}
		else if (soldBy.equals("Liferay China") ||
				 soldBy.equals("Liferay Singapore")) {

			return SupportRegionConstants.CHINA;
		}
		else if (soldBy.equals("Liferay India")) {
			return SupportRegionConstants.INDIA;
		}
		else if (soldBy.equals("Liferay Japan")) {
			return SupportRegionConstants.JAPAN;
		}
		else if (soldBy.equals("Liferay Spain")) {
			if (Validator.isNotNull(countryName) &&
				(countryName.equals("Cyprus") || countryName.equals("Greece") ||
				 countryName.equals("Italy"))) {

				return SupportRegionConstants.HUNGARY;
			}

			return SupportRegionConstants.SPAIN;
		}

		return SupportRegionConstants.GLOBAL;
	}

}