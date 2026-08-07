/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.exception;

/**
 * @author Felipe Franca
 */
public class AmbiguousContractChainException extends Exception {

	public AmbiguousContractChainException() {
	}

	public AmbiguousContractChainException(String message) {
		super(message);
	}

	public AmbiguousContractChainException(
		String message, Throwable throwable) {

		super(message, throwable);
	}

	public AmbiguousContractChainException(Throwable throwable) {
		super(throwable);
	}

}