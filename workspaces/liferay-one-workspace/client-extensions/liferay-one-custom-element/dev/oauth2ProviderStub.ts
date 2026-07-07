/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

/**
 * Dev-only stand-in for the portal-runtime module
 * `@liferay/oauth2-provider-web/client`, which is marked external for the
 * production build and does not exist in node_modules. Without it the Vite dev
 * server (/one-dev) cannot resolve the import and returns a 500 for every page
 * that reaches a spring-boot service.
 *
 * The dev page runs embedded in the portal origin, so this stub reuses the
 * live session cookie instead of performing the OAuth2 token exchange. Reads
 * work; writes succeed when the endpoint accepts session authentication. It is
 * never bundled into the production build.
 */

type AnyLiferay = {
	ThemeDisplay: {getPathContext: () => string};
};

const getLiferay = (): AnyLiferay =>
	(window as unknown as {Liferay: AnyLiferay}).Liferay;

async function getUserAgentApplicationHomePageURL(
	userAgentApplicationName: string
): Promise<string> {
	const pathContext = getLiferay().ThemeDisplay.getPathContext();

	const response = await fetch(
		`${pathContext}/o/oauth2/application` +
			`?externalReferenceCode=${userAgentApplicationName}`
	);

	const data = await response.json();

	return data.homePageURL as string;
}

class DevOAuth2Client {
	private readonly _homePageURL: string;

	constructor(homePageURL: string) {
		this._homePageURL = homePageURL;
	}

	async fetch(resource: string, options: RequestInit = {}): Promise<Response> {
		let resourceUrl = resource;

		if (!resourceUrl.startsWith(this._homePageURL)) {
			if (resourceUrl.startsWith('/')) {
				resourceUrl = resourceUrl.substring(1);
			}

			resourceUrl = `${this._homePageURL}/${resourceUrl}`;
		}

		return fetch(resourceUrl, {...options, credentials: 'include'});
	}
}

export async function FromUserAgentApplication(
	userAgentApplicationName: string
): Promise<DevOAuth2Client> {
	const homePageURL = await getUserAgentApplicationHomePageURL(
		userAgentApplicationName
	);

	return new DevOAuth2Client(homePageURL);
}
