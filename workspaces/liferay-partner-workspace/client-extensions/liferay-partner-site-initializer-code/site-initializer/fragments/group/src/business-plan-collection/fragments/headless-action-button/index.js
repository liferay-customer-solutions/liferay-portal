/* eslint-disable no-undef */

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const actionUrl = configuration.actionUrl;
const headlessActionButton = fragmentElement.querySelector('#headlessActionButton_' + fragmentEntryLinkNamespace);
const redirectUrl = configuration.redirectUrl;

const parseAndReplaceVariables = (string, objectEntry) => {
	const regex = /\$\{([\w.]+)\}/g;

	return string.replace(regex, (match, variableName) => {
		const value = objectEntry[variableName];

		return value !== undefined ? value : match;
	});
}

headlessActionButton.onclick = async () => {
	const objectDefinitionId = headlessActionButton.getAttribute('data-object-definition-id');
	const objectEntry = {};
	const objectEntryId = headlessActionButton.getAttribute('data-object-entry-id');

	if (objectDefinitionId != '') {
		const objectDefinitionResponse = await fetch(
			'/o/object-admin/v1.0/object-definitions/' + objectDefinitionId,
			{
				headers: {
					'content-type': 'application/json',
					'x-csrf-token': Liferay.authToken,
				},
				method: 'GET',
			}
		);
	
		const objectDefinition = await objectDefinitionResponse.json();

		if (objectEntryId != '') {
			const objectEntryResponse = await fetch(
				objectDefinition.restContextPath + '/' + objectEntryId,
				{
					headers: {
						'content-type': 'application/json',
						'x-csrf-token': Liferay.authToken,
					},
					method: 'GET',
				}
			);
		
			objectEntry = await objectEntryResponse.json();
		}
	}

	if (actionUrl != '') {
		const actionResponse = await fetch(
			parseAndReplaceVariables(actionUrl, objectEntry),
			{
				body: configuration.actionBody != '' ? parseAndReplaceVariables(configuration.actionBody, objectEntry) : '{}',
				headers: {
					'content-type': 'application/json',
					'x-csrf-token': Liferay.authToken,
				},
				method: configuration.actionMethod != '' ? configuration.actionMethod : 'GET',
			}
		);

		const action = await actionResponse.json();
	
		if (!actionResponse.ok) {
			console.error(action);
	
			Liferay.Util.openToast({
				message: 'We encountered an error executing your action.',
				type: 'danger',
			});
	
			return;
		}

		if (redirectUrl != '' && configuration.redirectUseActionResponse) {
			location.href = parseAndReplaceVariables(redirectUrl, action);

			return;
		}
	}

	if (redirectUrl != '') {
		location.href = parseAndReplaceVariables(redirectUrl, objectEntry);
	}
}