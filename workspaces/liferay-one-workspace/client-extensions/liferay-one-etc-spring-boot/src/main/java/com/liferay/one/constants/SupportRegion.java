/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.constants;

/**
 * @author Felipe Veloso
 */
public enum SupportRegion {

	AUSTRALIA("Australia"), BRAZIL("Brazil"), CHINA("China"), GLOBAL("Global"),
	HUNGARY("Hungary"), INDIA("India"), JAPAN("Japan"), SPAIN("Spain"),
	UNITED_STATES("United States");

	@Override
	public String toString() {
		return _value;
	}

	private SupportRegion(String value) {
		_value = value;
	}

	private final String _value;

}