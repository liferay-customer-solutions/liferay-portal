/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const envvar: Record<string, any> =
	(typeof process !== 'undefined' && process.env) ||
	(typeof import.meta !== 'undefined' && (import.meta as any).env) ||
	{};

const rawFeatureFlags = envvar.VITE_FEATURE_FLAGS;

const env = {
	aboutTheOverviewPageURL: envvar.VITE_ABOUT_THE_OVERVIEW_PAGE_URL || '',
	accountSettingsURL: envvar.VITE_ACCOUNT_SETTINGS_URL || '',
	articleAccountSupportURL: envvar.VITE_ARTICLE_ACCOUNT_SUPPORT_URL || '',
	articleDeactivateKey: envvar.VITE_ARTICLE_DEACTIVATE_KEY_URL || '',
	articleDeployingActivationKeysURL:
		envvar.VITE_ARTICLE_DEPLOYING_ACTIVATION_KEYS_URL || '',
	articleGettingStartedWithLiferayEnterpriseSearchURL:
		envvar.VITE_ARTICLE_GETTING_STARTED_WITH_LIFERAY_ENTERPRISE_SEARCH_URL ||
		'',
	articleNotifiedWhenMyActivationKeyIsAboutToExpireURL:
		envvar.VITE_ARTICLE_NOTIFIED_WHEN_MY_ACTIVATION_KEY_IS_ABOUT_TO_EXPIRE_URL ||
		'',
	articleWhatIsMyInstanceSizingValueURL:
		envvar.VITE_ARTICLE_WHAT_IS_MY_INSTANCE_SIZING_VALUE_URL || '',
	featureFlags: rawFeatureFlags
		? rawFeatureFlags.split(',')
		: ([] as string[]),
	gravatarAPI: envvar.VITE_GRAVATAR_API || '',
	helpCenterURL: envvar.VITE_HELP_CENTER_URL || '',
	importDate: envvar.VITE_IMPORT_DATE || null,
	provisioningServerAPI: envvar.VITE_PROVISIONING_SERVER_API || '',
	submitSupportTicketURL: envvar.VITE_SUBMIT_SUPPORT_TICKET_URL || '',
};

export default env;
