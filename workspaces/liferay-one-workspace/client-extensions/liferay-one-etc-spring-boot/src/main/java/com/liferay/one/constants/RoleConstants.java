/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.constants;

/**
 * @author Felipe Franca
 * @author Felipe Veloso
 */
public class RoleConstants {

	public static final String ERC_PROJECT_ADMIN = "C_PROJECT_ADMIN";

	public static final String ERC_PROJECT_REQUESTER = "C_PROJECT_REQUESTER";

	public static final String ERC_PROJECT_USER = "C_PROJECT_USER";

	public static final String[] ERCS_SUPPORT_PROJECT = {
		ERC_PROJECT_ADMIN, ERC_PROJECT_REQUESTER, ERC_PROJECT_USER
	};

	public static final String[] ERCS_SUPPORT_PROJECT_TICKET = {
		ERC_PROJECT_ADMIN, ERC_PROJECT_REQUESTER
	};

	public static final String NAME_ACCOUNT_ADMINISTRATOR =
		"Account Administrator";

	public static final String NAME_ACCOUNT_MEMBER = "Account Member";

	public static final String NAME_ACCOUNT_REQUESTER = "Account Requester";

	public static final String NAME_ADMINISTRATOR = "Administrator";

	public static final String NAME_CLOUD_NATIVE_CONTACT =
		"Cloud Native Contact";

	public static final String NAME_LIFERAY_SALES = "Liferay Sales";

	public static final String NAME_LIFERAY_STAFF = "Liferay Staff";

	public static final String NAME_PARTNER_MANAGER = "Partner Manager";

	public static final String NAME_PARTNER_MARKETING_USER =
		"Partner Marketing User";

	public static final String NAME_PARTNER_MEMBER = "Partner Member";

	public static final String NAME_PARTNER_SALES_USER = "Partner Sales User";

	public static final String NAME_PARTNER_TECHNICAL_USER =
		"Partner Technical User";

	public static final String NAME_PROVISIONING_ADMINISTRATOR =
		"Provisioning Administrator";

	public static final String NAME_PROVISIONING_MEMBER = "Provisioning Member";

	public static final String NAME_SUPPORT_ADMINISTRATOR =
		"Support Administrator";

	public static final String[] NAMES_CUSTOMER_ACCOUNT_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_MEMBER, NAME_ACCOUNT_REQUESTER,
		NAME_SUPPORT_ADMINISTRATOR
	};

	public static final String[] NAMES_MANAGE_LICENSE_KEYS = {
		NAME_LIFERAY_SALES, NAME_PARTNER_MANAGER, NAME_SUPPORT_ADMINISTRATOR
	};

	public static final String[] NAMES_PARTNER_ACCOUNT_ROLES = {
		NAME_PARTNER_MANAGER, NAME_PARTNER_MARKETING_USER, NAME_PARTNER_MEMBER,
		NAME_PARTNER_SALES_USER, NAME_PARTNER_TECHNICAL_USER
	};

	public static final String[] NAMES_SUPPORT_ACCOUNT = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_MEMBER, NAME_ACCOUNT_REQUESTER
	};

	public static final String[] NAMES_SUPPORT_ACCOUNT_TICKET = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_REQUESTER
	};

}