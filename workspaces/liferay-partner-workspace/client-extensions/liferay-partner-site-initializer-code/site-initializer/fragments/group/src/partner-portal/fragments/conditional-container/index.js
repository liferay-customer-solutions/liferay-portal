/* eslint-disable no-undef */

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const valuesToHide = configuration.valuesToHide;
const conditionalElement = fragmentElement.querySelector('#conditional_container_' + fragmentEntryLinkNamespace);
const field = configuration.field + ".key";
const hideOnMatch = configuration.hideOnMatch;

const elementToHide = async () => {
	if (conditionalElement === null) {
		return;
	}

	const objectDefinitionId = conditionalElement.getAttribute('data-object-definition-id');
	const objectEntryId = conditionalElement.getAttribute('data-object-entry-id');

	if (objectDefinitionId === '') {
		return;
	}

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

	if (objectEntryId === '') {
		return;
	}

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

	const objectEntry = await objectEntryResponse.json();

	let object = objectEntry;
	let status = field;
	let statusArray = field.split('.');

	for (let i = 0; i < statusArray.length; i++) {
		status = object[statusArray[i]];

		object = object[statusArray[i]];
	}

	const formItems = document.querySelector('#conditional_container_' + fragmentEntryLinkNamespace);

	if (objectEntry === null || valuesToHide.includes(status)) {
		if (hideOnMatch === true) {
			formItems.remove();	
		}
		return;
	}
	if (hideOnMatch === false) {
			formItems.remove();	
		}
	return;
}

elementToHide();