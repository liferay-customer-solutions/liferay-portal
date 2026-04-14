/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.constants;

import java.util.Map;

/**
 * @author Felipe Veloso
 */
public class AccountSubscriptionGroupConstants {

	public static final String ACTIVATION_PRODUCT_NAME_CLOUD_NATIVE =
		"Cloud Native";

	public static final String ACTIVATION_PRODUCT_NAME_DXP = "DXP";

	public static final String ACTIVATION_PRODUCT_NAME_LIFERAY_PAAS =
		"Liferay PaaS";

	public static final String ACTIVATION_PRODUCT_NAME_LIFERAY_SAAS =
		"Liferay SaaS";

	public static final String NAME_ANALYTICS_CLOUD = "Analytics Cloud";

	public static final String NAME_COMMERCE = "Commerce";

	public static final String NAME_COMMERCE_FOR_CLOUD = "Commerce for Cloud";

	public static final String NAME_ENTERPRISE_SEARCH = "Enterprise Search";

	public static final String NAME_LIFERAY_CLOUD = "Liferay Cloud";

	public static final String NAME_LIFERAY_SELF_HOSTED = "Liferay Self-Hosted";

	public static final String NAME_PARTNERSHIP = "Partnership";

	public static final String NAME_PORTAL = "Portal";

	public static final String[] NAMES_ACTIVATION = {
		NAME_ANALYTICS_CLOUD, NAME_COMMERCE, NAME_COMMERCE_FOR_CLOUD,
		NAME_ENTERPRISE_SEARCH, NAME_LIFERAY_CLOUD, NAME_LIFERAY_SELF_HOSTED,
		NAME_PORTAL
	};

	public static final int ORDER_DEFAULT = 100000;

	public static final Map<String, Integer> menuOrders = Map.of(
		NAME_LIFERAY_CLOUD, 100, NAME_COMMERCE_FOR_CLOUD, 200,
		NAME_LIFERAY_SELF_HOSTED, 300, NAME_ANALYTICS_CLOUD, 400, NAME_COMMERCE,
		500, NAME_ENTERPRISE_SEARCH, 600, NAME_PORTAL, 700);
	public static final Map<String, Integer> tabOrders = Map.of(
		NAME_PARTNERSHIP, 100, NAME_LIFERAY_CLOUD, 200, NAME_COMMERCE_FOR_CLOUD,
		400, NAME_LIFERAY_SELF_HOSTED, 500, NAME_ANALYTICS_CLOUD, 600,
		NAME_COMMERCE, 700, NAME_ENTERPRISE_SEARCH, 800, NAME_PORTAL, 900);

}