/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.model;

import com.liferay.one.constants.EntitlementConstants;

import java.util.List;
import java.util.Objects;

/**
 * The number of Liferay Data Platform events a project is entitled to, rolled
 * up from its <code>events</code> and <code>events-add-on-bucket</code>
 * entitlements. An add-on bucket entitlement counts buckets, so its quantity
 * is multiplied by the bucket size the usage definition declares.
 *
 * @author Drew Brokke
 */
public class LDPEventAllotment {

	public LDPEventAllotment(
		List<Entitlement> entitlements, long overageBucketSize) {

		long addOnBucketCount = 0;
		long baseQuantity = 0;
		boolean unlimited = false;

		for (Entitlement entitlement : entitlements) {
			String name = entitlement.getName();

			if (!name.equals(EntitlementConstants.NAME_EVENTS) &&
				!name.equals(EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET)) {

				continue;
			}

			if (Objects.equals(
					entitlement.getGrantType(),
					EntitlementConstants.GRANT_TYPE_UNLIMITED)) {

				unlimited = true;

				continue;
			}

			Double quantity = entitlement.getQuantity();

			if (quantity == null) {
				continue;
			}

			if (quantity < 0) {
				unlimited = true;

				continue;
			}

			if (name.equals(EntitlementConstants.NAME_EVENTS)) {
				baseQuantity += quantity.longValue();
			}

			if (name.equals(EntitlementConstants.NAME_EVENTS_ADD_ON_BUCKET)) {
				addOnBucketCount += quantity.longValue();
			}
		}

		_overageBucketSize = overageBucketSize;

		_addOnBucketCount = addOnBucketCount;
		_baseQuantity = baseQuantity;
		_unlimited = unlimited;
	}

	public long getAddOnBucketCount() {
		return _addOnBucketCount;
	}

	public long getAddOnQuantity() {
		return _addOnBucketCount * _overageBucketSize;
	}

	public long getBaseQuantity() {
		return _baseQuantity;
	}

	public long getEntitledQuantity() {
		return _baseQuantity + getAddOnQuantity();
	}

	public boolean isUnlimited() {
		return _unlimited;
	}

	private final long _addOnBucketCount;
	private final long _baseQuantity;
	private final long _overageBucketSize;
	private final boolean _unlimited;

}