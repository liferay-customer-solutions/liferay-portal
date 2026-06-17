/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

/**
 * @author Felipe Franca
 */
public class JiraOrganization {

	public JiraOrganization(String externalKey, String id, String name) {
		_externalKey = externalKey;
		_id = id;
		_name = name;
	}

	public String getExternalKey() {
		return _externalKey;
	}

	public String getId() {
		return _id;
	}

	public String getName() {
		return _name;
	}

	private final String _externalKey;
	private final String _id;
	private final String _name;

}