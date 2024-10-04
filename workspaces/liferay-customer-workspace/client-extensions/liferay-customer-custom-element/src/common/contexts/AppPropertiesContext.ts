/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ApolloClient} from '@apollo/client';
import {createContext, useContext} from 'react';

export interface IProps {
	accountSettingsURL: string;
	articleAccountSupportURL: string;
	articleDeactivateKey: string;
	articleDeployingActivationKeysURL: string;
	articleGettingStartedWithLiferayEnterpriseSearchURL: string;
	articleNotifiedWhenMyActivationKeyIsAboutToExpireURL: string;
	articleWhatIsMyInstanceSizingValueURL: string;
	client: ApolloClient<any>;
	featureFlags: string[];
	gravatarAPI: string;
	importDate: string | null;
	oktaSessionAPI: string;
	provisioningServerAPI: string;
	submitSupportTicketURL: string;
	theOverviewPageURL: string;
}

export const AppPropertiesContext = createContext<IProps>({} as IProps);

export function useAppPropertiesContext() {
	const context = useContext(AppPropertiesContext);

	return context;
}
