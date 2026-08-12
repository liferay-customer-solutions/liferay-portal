/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.exception;

/**
 * @author Felipe Veloso
 */
public class ProjectNotFoundException extends Exception {

	public ProjectNotFoundException() {
	}

	public ProjectNotFoundException(String message) {
		super(message);
	}

	public ProjectNotFoundException(Throwable throwable) {
		super(throwable);
	}

}