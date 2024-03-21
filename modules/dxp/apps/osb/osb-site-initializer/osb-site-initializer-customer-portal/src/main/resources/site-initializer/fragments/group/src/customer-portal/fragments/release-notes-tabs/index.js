/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const dropdown = fragmentElement.querySelector('.navbar-collapse');
const dropdownButton = fragmentElement.querySelector('.navbar-toggler-link');
const editMode = layoutMode === 'edit';
const persistedTabKey = 'tabsFragment_' + '_persistedTabId';
const currentURL = window.location.href;
const desiredValue = currentURL.match(/&t=([^&]*)/);

const tabItems = [].slice.call(
	fragmentElement.querySelectorAll(
		'[data-fragment-namespace="' + fragmentNamespace + '"].nav-link'
	)
);

const tabPanelItems = [].slice.call(
	fragmentElement.querySelectorAll(
		'[data-fragment-namespace="' + fragmentNamespace + '"].tab-panel-item'
	)
);

const persistedTab = (function () {
	if (!configuration.persistSelectedTab) {
		let persistedId;

		return {
			getId() {
				return persistedId;
			},

			setId(nextId) {
				persistedId = nextId;
			},
		};
	}

	return {
		getId() {
			return Number(
				Liferay.Util.SessionStorage.getItem(
					persistedTabKey,
					Liferay.Util.SessionStorage.TYPES.PERSONALIZATION
				)
			);
		},

		setId(id) {
			Liferay.Util.SessionStorage.setItem(
				persistedTabKey,
				id,
				Liferay.Util.SessionStorage.TYPES.PERSONALIZATION
			);
		},
	};
})();

function activeTab(item) {
	tabItems.forEach((tabItem) => {
		tabItem.setAttribute('aria-selected', false);
		tabItem.classList.remove('active');
	});

	item.setAttribute('aria-selected', true);
	item.classList.add('active');
}

function activeTabPanel(item) {
	tabPanelItems.forEach((tabPanelItem) => {
		if (!tabPanelItem.classList.contains('d-none')) {
			tabPanelItem.classList.add('d-none');
		}
	});

	item.classList.remove('d-none');
}

function handleDropdown(event, item) {
	event.preventDefault();
	dropdown.classList.toggle('show');

	const ariaExpanded = dropdownButton.getAttribute('aria-expanded');

	dropdownButton.setAttribute(
		'aria-expanded',
		ariaExpanded === 'false' ? true : false
	);

	if (item) {
		handleDropdownButtonName(item);
	}
}

function handleDropdownButtonName(item) {
	const tabText =
		item.querySelector('lfr-editable') ||
		item.querySelector('.navbar-text-truncate');

	if (tabText) {
		dropdownButton.querySelector('.navbar-text-truncate').innerHTML =
			tabText.textContent;
	}
}

function openTabPanel(event, i) {
	const currentTarget = event.currentTarget;
	const target = event.target;

	const isEditable =
		target.hasAttribute('data-lfr-editable-id') ||
		target.hasAttribute('contenteditable');

	const dropdownIsOpen = JSON.parse(
		dropdownButton.getAttribute('aria-expanded')
	);

	if (!isEditable || !editMode) {
		if (dropdownIsOpen) {
			handleDropdown(event, currentTarget);
		} else {
			handleDropdownButtonName(currentTarget);
		}

		currentTarget.focus();

		activeTab(currentTarget, i);
		activeTabPanel(tabPanelItems[i]);
		persistedTab.setId(i);

		Liferay.fire('tabsFragment:activePanel', {panel: tabPanelItems[i]});
	}
}

function addURL(addition) {
	if (!editMode) {
		let currentURL = window.location.href;
		const index = currentURL.indexOf('&t=');

		if (index !== -1) {
			currentURL = currentURL.substring(0, index);
		}

		const newURL = currentURL + '&t=' + addition;
		window.location.href = newURL;
	}
}

function main() {
	const tabItemId = tabItems[persistedTab.getId()] ? persistedTab.getId() : 0;

	const structureReleaseNotes = [
		'RELEASE-NOTES-HIGHLIGHT-STRUCTURE',
		'RELEASE-NOTES-FEATURE-STRUCTURE',
		'RELEASE-NOTES-BREAKING-CHANGE-STRUCTURE',
	];

	if (!editMode) {
		tabItems.forEach((item, index) => {
			item.addEventListener('click', () => {
				addURL(structureReleaseNotes[index]);
				persistedTab.setId(index);
			});
		});

		tabItems.forEach((item, index) => {
			if (structureReleaseNotes[index] === desiredValue[1]) {
				activeTab(tabItems[index]);
			}
		});
	}

	dropdownButton.addEventListener('click', (event) => {
		handleDropdown(event);
	});

	if (editMode) {
		tabItems.forEach((item, index) => {
			item.addEventListener('click', (event) => {
				openTabPanel(event, index);
			});
		});
	}

	activeTabPanel(tabPanelItems[tabItemId]);
	handleDropdownButtonName(tabItems[tabItemId]);
}

main();