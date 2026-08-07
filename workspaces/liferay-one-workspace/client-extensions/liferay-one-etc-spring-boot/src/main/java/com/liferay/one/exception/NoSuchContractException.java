/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.exception;

/**
 * @author Felipe Franca
 */
public class NoSuchContractException extends Exception {

	public NoSuchContractException() {
	}

	public NoSuchContractException(String message) {
		super(message);
	}

	public NoSuchContractException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public NoSuchContractException(Throwable throwable) {
		super(throwable);
	}

}