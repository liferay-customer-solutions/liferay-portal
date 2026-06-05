/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import org.json.JSONObject;

/**
 * @author Felipe Veloso
 */
public class CommerceOrder extends LiferayObject {

	public CommerceOrder(JSONObject jsonObject) {
		super(jsonObject);

		_commerceOrderId = jsonObject.getLong("id");
		_contractId = getCustomFieldLong("contractId");
	}

	public long getCommerceOrderId() {
		return _commerceOrderId;
	}

	public long getContractId() {
		return _contractId;
	}

	private final long _commerceOrderId;
	private final long _contractId;

}