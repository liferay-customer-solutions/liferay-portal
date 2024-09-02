/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.partner.utils;

import java.util.ArrayList;

/**
 * @author Felipe Franca
 */
public class Constants {

    public static ArrayList<String> getChannelRoles() {
        ArrayList<String> channelRoles = new ArrayList<String>();

        channelRoles.add(CHANNEL_ACCOUNT_MANAGER);
        channelRoles.add(CHANNEL_ENABLEMENT_MANAGER);
        channelRoles.add(CHANNEL_FINANCE_MANAGER);
        channelRoles.add(CHANNEL_MARKETING_DIRECTOR);
        channelRoles.add(CHANNEL_MARKETING_MANAGER);
        channelRoles.add(CHANNEL_OPERATIONS_MANAGER);

        return channelRoles;
    }

    public static ArrayList<String> getPartnerRoles() {
        ArrayList<String> partnerRoles = new ArrayList<String>();

        partnerRoles.add(PARTNER_MANAGER);
        partnerRoles.add(PARTNER_MARKETING_USER);
        partnerRoles.add(PARTNER_SALES_USER);
        partnerRoles.add(PARTNER_TECHNICAL_USER);

        return partnerRoles;
    }

    public static final String ADMINISTRATOR = "Administrator";

    public static final String CHANNEL = "Channel";
    public static final String CHANNEL_ACCOUNT_MANAGER = "Channel Account Manager (CAM)";
    public static final String CHANNEL_ENABLEMENT_MANAGER = "Channel Enablement Manager (CEM)";
    public static final String CHANNEL_FINANCE_MANAGER = "Channel Finance Manager (CFM)";
    public static final String CHANNEL_MARKETING_DIRECTOR = "Channel Marketing Director (CMD)";
    public static final String CHANNEL_MARKETING_MANAGER = "Channel Marketing Manager (CMM)";
    public static final String CHANNEL_OPERATIONS_MANAGER = "Channel Operations Manager (COM)";

    public static final String LEAD_PROXY_EXTERNAL_REFERENCE_CODE = "LeadProxy";

    public static final String OPPORTUNITY_PARTNER_ROLE_PROXY_EXTERNAL_REFERENCE_CODE = "OpportunityPartnerRoleProxy";

    public static final String PARTNER = "Partner";
    public static final String PARTNER_MANAGER = "Partner Manager (PM)";
    public static final String PARTNER_MARKETING_USER = "Partner Marketing User (PMU)";
    public static final String PARTNER_SALES_USER = "Partner Sales User (PSU)";
    public static final String PARTNER_TECHNICAL_USER = "Partner Technical User (PTU)";

    public static final String UNAUTHORIZED = "Unauthorized";

}
