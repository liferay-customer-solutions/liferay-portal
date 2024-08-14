/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const DROPDOWN_SELECT = 'input[name="r_campaign_c_campaignId"]';
const FORM_CONTAINER =
	'#_com_liferay_object_web_internal_object_definitions_portlet_ObjectDefinitionsPortlet_41445_fm';
const FORM_GROUP = '.form-group';
const HIDDEN_CLASS = 'hidden';
const ITEM_OPTION = '.dropdown-item';
const SELECT_OPTION = '.dropdown-menu.autocomplete-dropdown-menu.show';

document.addEventListener('DOMContentLoaded', () => {
	const handleMutations = (mutations) => {
		mutations.forEach(({addedNodes}) => {
			if (addedNodes.length) {
				const selectDropdown = document.querySelector(DROPDOWN_SELECT);

				if (selectDropdown) {
					selectDropdown
						.closest(FORM_GROUP)
						.classList.add(HIDDEN_CLASS);

					setTimeout(() => {
						selectDropdown.dispatchEvent(new Event('focus'));

						const selectOption =
							document.querySelector(SELECT_OPTION);

						if (selectOption) {
							const itemOption =
								selectOption.querySelector(ITEM_OPTION);

							if (itemOption) {
								itemOption.click();
							}
						}
					}, 200);
				}
			}
		});
	};

	const observer = () => {
		const formContainer = document
			.querySelector(FORM_CONTAINER)
			?.closest('form');

		if (formContainer) {
			const mutationObserver = new MutationObserver(handleMutations);
			mutationObserver.observe(formContainer, {
				childList: true,
				subtree: true,
			});
		}
	};

	observer();
});
