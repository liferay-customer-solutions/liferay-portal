/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.exception;

/**
 * @author Ryan Schuhler
 */
public class InvalidUsageParameterException extends Exception {

	public InvalidUsageParameterException() {
	}

	public InvalidUsageParameterException(String message) {
		super(message);
	}

	public InvalidUsageParameterException(String message, Throwable throwable) {
		super(message, throwable);
	}

}