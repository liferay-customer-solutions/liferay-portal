/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.constants;

/**
 * @author Amos Fong
 */
public class EnvironmentConstants {

	public static final String ACTIVATION_MODE_OFFLINE = "offline";

	public static final String ACTIVATION_MODE_ONLINE = "online";

	public static final String ACTIVATION_STATUS_ACTIVE = "active";

	public static final String ACTIVATION_STATUS_PENDING = "pending";

	public static final String OFFERING_ANALYTICS_CLOUD = "Analytics Cloud";

	public static final String OFFERING_CLOUD_NATIVE = "Cloud Native";

	public static final String OFFERING_PAAS = "PaaS";

	public static final String OFFERING_SAAS = "SaaS";

	public static final String PROFILE_ANALYTICS_CLOUD = "analytics-cloud";

	public static final String PROFILE_PAAS = "paas";

	public static final String PROFILE_SAAS = "saas";

	public static final String[] PROFILES = {
		PROFILE_ANALYTICS_CLOUD, PROFILE_PAAS, PROFILE_SAAS
	};

	public static final String TYPE_NONPRODUCTION = "non-production";

	public static final String TYPE_PRODUCTION = "production";

	public static final String TYPE_UAT = "uat";

}