/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ApolloProvider} from '@apollo/client';
import {ClayIconSpriteContext} from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import React from 'react';
import {Root, createRoot} from 'react-dom/client';
import {SWRConfig} from 'swr';

import {AppPropertiesContext} from './contexts/AppPropertiesContext';
import AttachmentUploader from './features/attachment-uploader';
import Onboarding from './features/onboarding';
import Project from './features/project';
import Projects from './features/projects';
import SecurityVulnerabilities from './features/security-vulnerabilities';
import useApollo from './hooks/useApollo';
import useGlobalNetworkIndicator from './hooks/useGlobalNetworkIndicator';
import env from './utils/env';
import getIconSpriteMap from './utils/getIconSpriteMap';
import swrCacheProvider from './utils/swrCacheProvider';

import './main.css';

const ELEMENT_ID = 'liferay-customer-custom-element';

const AppRoutes = {
	attachmentUploader: AttachmentUploader,
	onboarding: Onboarding,
	project: Project,
	projects: Projects,
	securityVulnerabilities: SecurityVulnerabilities,
};

type APIs = {
	gravatarAPI: string | null;
	provisioningServerAPI: string | null;
};

type CustomerPortalAppProps = {
	apis: APIs;
	route: string;
};

const CustomerPortalApp: React.FC<CustomerPortalAppProps> = ({apis, route}) => {
	const {client, networkStatus} = useApollo(
		apis.provisioningServerAPI as string
	);

	useGlobalNetworkIndicator(networkStatus);

	if (!client) {
		return <ClayLoadingIndicator />;
	}

	const AppRouteComponent = (AppRoutes as any)[route];

	return (
		<ApolloProvider client={client}>
			<AppPropertiesContext.Provider
				value={
					{
						...env,
						...apis,
						client,
					} as any
				}
			>
				{env.featureFlags?.includes('LPS-192494')}

				<AppRouteComponent />
			</AppPropertiesContext.Provider>
		</ApolloProvider>
	);
};

class CustomerPortalWebComponent extends HTMLElement {
	private root: Root | undefined;

	connectedCallback() {
		const apis = {
			gravatarAPI: env.gravatarAPI,
			provisioningServerAPI: env.provisioningServerAPI,
		};

		if (!this.root) {
			this.root = createRoot(this);

			this.root.render(
				<ClayIconSpriteContext.Provider value={getIconSpriteMap()}>
					<SWRConfig
						value={{
							provider: swrCacheProvider,
							revalidateOnFocus: false,
						}}
					>
						<CustomerPortalApp
							apis={apis}
							route={super.getAttribute('route') as string}
						/>
					</SWRConfig>
				</ClayIconSpriteContext.Provider>
			);
		}
	}
}

if (!customElements.get(ELEMENT_ID)) {
	customElements.define(ELEMENT_ID, CustomerPortalWebComponent);
}
