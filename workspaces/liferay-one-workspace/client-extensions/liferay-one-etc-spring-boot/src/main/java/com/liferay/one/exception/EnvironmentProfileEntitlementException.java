/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.exception;

import com.liferay.petra.string.StringBundler;

/**
 * @author Felipe Franca
 */
public class EnvironmentProfileEntitlementException extends Exception {

	public EnvironmentProfileEntitlementException(
		String environmentProfile, String projectExternalReferenceCode) {

		super(
			StringBundler.concat(
				"Project ", projectExternalReferenceCode,
				" does not have an active entitlement for the environment ",
				"profile ", environmentProfile));
	}

}