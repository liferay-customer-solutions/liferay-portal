/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const editMode = layoutMode === 'edit';
const previewMode = layoutMode === 'preview';

(function () {
	if (typeof Storage !== 'undefined') {
		const key = 'tabsFragment__persistedTabId';
		const value = '0';
		Liferay.Util.SessionStorage.setItem(
			key,
			value,
			Liferay.Util.SessionStorage.TYPES.PERSONALIZATION
		);
	}
})();

function onRedirect(id) {
	let currentURL = window.location.href;
	const index = currentURL.indexOf('dxp-release-notes');

	if (index !== -1) {
		currentURL = currentURL.substring(0, index);
	}

	const newURL =
		currentURL + 'v/' + id + '&t=' + 'RELEASE-NOTES-HIGHLIGHT-STRUCTURE';
	window.location.href = newURL;
}

const releaseCategoryId = document
	.querySelector('p')
	.getAttribute('data-release-category-id');

if (!previewMode && !editMode) {
	onRedirect(releaseCategoryId + '?r=' + releaseCategoryId);
}