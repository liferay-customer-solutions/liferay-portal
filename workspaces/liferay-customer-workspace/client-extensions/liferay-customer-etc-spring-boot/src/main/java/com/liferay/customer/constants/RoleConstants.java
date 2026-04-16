/**
 * SPDX-FileCopyrightText: (c) 2024 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.constants;

/**
 * @author Felipe Franca
 */
public class RoleConstants {

	public static final String NAME_ACCOUNT_ADMINISTRATOR =
		"Account Administrator";

	public static final String NAME_ACCOUNT_MEMBER = "Account Member";

	public static final String NAME_ADMINISTRATOR = "Administrator";

	public static final String NAME_CRITICAL_INCIDENT_CONTACT =
		"Critical Incident Contact";

	public static final String NAME_CUSTOMER_EXPERIENCE_MANAGER =
		"Customer Experience Manager";

	public static final String NAME_DATA_BREACH_CONTACT = "Data Breach Contact";

	public static final String NAME_LIFERAY_STAFF = "Liferay Staff";

	public static final String NAME_PARTNER = "Partner";

	public static final String NAME_PARTNER_MANAGER = "Partner Manager";

	public static final String NAME_PARTNER_MARKETING_USER =
		"Partner Marketing User";

	public static final String NAME_PARTNER_MEMBER = "Partner Member";

	public static final String NAME_PARTNER_SALES_USER = "Partner Sales User";

	public static final String NAME_PARTNER_TECHNICAL_USER =
		"Partner Technical User";

	public static final String NAME_PROVISIONING = "Provisioning";

	public static final String NAME_PROVISIONING_MEMBER = "Provisioning Member";

	public static final String NAME_REQUESTER = "Requester";

	public static final String NAME_SECURITY_INCIDENT_CONTACT =
		"Security Incident Contact";

	public static final String NAME_SOLUTION_ARCHITECT = "Solution Architect";

	public static final String[] NAMES_CONTACT_ACCOUNT_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_MEMBER,
		NAME_CRITICAL_INCIDENT_CONTACT, NAME_CUSTOMER_EXPERIENCE_MANAGER,
		NAME_DATA_BREACH_CONTACT, NAME_PARTNER_MANAGER,
		NAME_PARTNER_MARKETING_USER, NAME_PARTNER_MEMBER,
		NAME_PARTNER_SALES_USER, NAME_PARTNER_TECHNICAL_USER, NAME_PROVISIONING,
		NAME_REQUESTER, NAME_SECURITY_INCIDENT_CONTACT, NAME_SOLUTION_ARCHITECT
	};

	public static final String[] NAMES_PARTNER_ACCOUNT_ROLES = {
		NAME_PARTNER_MANAGER, NAME_PARTNER_MARKETING_USER, NAME_PARTNER_MEMBER,
		NAME_PARTNER_SALES_USER, NAME_PARTNER_TECHNICAL_USER
	};

	public static final String[] NAMES_SUPPORT_ACCOUNT_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_ACCOUNT_MEMBER, NAME_REQUESTER
	};

	public static final String[] NAMES_SUPPORT_ACCOUNT_TICKET_ROLES = {
		NAME_ACCOUNT_ADMINISTRATOR, NAME_REQUESTER
	};

}