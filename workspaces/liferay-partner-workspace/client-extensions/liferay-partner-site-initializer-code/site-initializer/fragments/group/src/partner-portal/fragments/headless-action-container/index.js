/* eslint-disable no-eval */
/* eslint-disable no-undef */

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const headlessActionContainer = fragmentElement.querySelector('#headlessActionContainer_' + fragmentEntryLinkNamespace);

const parseAndReplaceVariables = (string, json) => {
	const regex = /\$\{([\w.]+)\}/g;

	return string.replace(regex, (match, variableName) => {
		const value = json[variableName];

		return value !== undefined ? value : match;
	});
}

headlessActionContainer.onclick = async () => {
	const actionBody = headlessActionContainer.getAttribute('data-action-body');
	const actionCallback = headlessActionContainer.getAttribute('data-action-callback');
	const actionUrl = headlessActionContainer.getAttribute('data-action-url');

	if (actionUrl !== '') {
		const actionResponse = await fetch(
			actionUrl,
			{
				body: actionBody !== '' ? actionBody : '{}',
				headers: {
					'content-type': 'application/json',
					'x-csrf-token': Liferay.authToken,
				},
				method: configuration.actionMethod !== '' ? configuration.actionMethod : 'GET',
			}
		).json();

		if (!actionResponse.ok) {
			console.error(actionResponse);
	
			Liferay.Util.openToast({
				message: 'We encountered an error executing your action.',
				type: 'danger',
			});
	
			return;
		}

		if (actionCallback !== '') {
			eval(actionCallback);
		}
	}
}