/* eslint-disable no-undef */

/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const editableStatuses = configuration.editableStatuses;
const formLockElement = fragmentElement.querySelector('#formLock_' + fragmentEntryLinkNamespace);
const statusField = configuration.statusField;

const lockFormItems = async () => {
	if (formLockElement == null) {
		return;
	}

	const objectDefinitionId = formLockElement.getAttribute('data-object-definition-id');
	const objectEntryId = formLockElement.getAttribute('data-object-entry-id');

	if (objectDefinitionId == '') {
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

	if (objectEntryId == '') {
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
	let status = statusField;
	let statusArray = statusField.split('.');

	for (let i = 0; i < statusArray.length; i++) {
		status = object[statusArray[i]];

		object = object[statusArray[i]];
	}

	if (objectEntry == null || editableStatuses.includes(status)) {
		return;
	}

	const formItems = document.querySelectorAll('button, input, select, textarea');

	formItems.forEach(item => {
		item.disabled = true;
	});
}

await lockFormItems();