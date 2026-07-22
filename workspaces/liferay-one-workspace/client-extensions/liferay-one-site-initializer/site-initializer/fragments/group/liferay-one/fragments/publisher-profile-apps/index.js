/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const appCards = fragmentElement.querySelectorAll('.publisher-profile-app-card');
const paginationBarElement = fragmentElement.querySelector(
	'.publisher-profile-apps-pagination-bar'
);

if (appCards.length && paginationBarElement) {
	const pageSizeSelect = paginationBarElement.querySelector(
		'.publisher-profile-apps-page-size'
	);
	const paginationElement = paginationBarElement.querySelector(
		'.publisher-profile-apps-pagination'
	);
	const resultsRangeElement = paginationBarElement.querySelector(
		'.publisher-profile-apps-results-range'
	);
	const resultsTotalElement = paginationBarElement.querySelector(
		'.publisher-profile-apps-results-total'
	);

	const spritemap = paginationElement.dataset.spritemap;

	let currentPageNumber = 1;

	const addPageItem = (content, pageNumber, disabled, active) => {
		const pageItem = document.createElement('li');

		pageItem.classList.add('page-item');

		if (active) {
			pageItem.classList.add('active');
		}

		if (disabled) {
			pageItem.classList.add('disabled');
		}

		const pageLink = document.createElement('button');

		pageLink.classList.add('page-link');
		pageLink.disabled = disabled;
		pageLink.type = 'button';

		if (typeof content === 'number') {
			pageLink.textContent = content;
		}
		else {
			pageLink.innerHTML = `<svg class="lexicon-icon" role="presentation"><use href="${spritemap}#${content}" /></svg>`;
		}

		if (pageNumber) {
			pageLink.addEventListener('click', () => {
				currentPageNumber = pageNumber;

				showPage();
			});
		}

		pageItem.appendChild(pageLink);

		paginationElement.appendChild(pageItem);
	};

	const showPage = () => {
		const pageSize = parseInt(pageSizeSelect.value, 10);

		const pagesCount = Math.max(Math.ceil(appCards.length / pageSize), 1);

		currentPageNumber = Math.min(currentPageNumber, pagesCount);

		appCards.forEach((appCard, index) => {
			appCard.classList.toggle(
				'publisher-profile-app-card-hidden',
				Math.floor(index / pageSize) !== currentPageNumber - 1
			);
		});

		resultsRangeElement.textContent = resultsRangeElement.dataset.template
			.replace('{0}', (currentPageNumber - 1) * pageSize + 1)
			.replace(
				'{1}',
				Math.min(currentPageNumber * pageSize, appCards.length)
			);

		resultsTotalElement.textContent =
			resultsTotalElement.dataset.template.replace(
				'{0}',
				appCards.length
			);

		paginationElement.innerHTML = '';

		addPageItem('angle-left', currentPageNumber - 1, currentPageNumber === 1, false);

		let previousPageNumberShown = 0;

		for (let pageNumber = 1; pageNumber <= pagesCount; pageNumber++) {
			if (
				(pageNumber !== 1) &&
				(pageNumber !== pagesCount) &&
				(Math.abs(pageNumber - currentPageNumber) > 2)
			) {
				continue;
			}

			if (pageNumber - previousPageNumberShown > 1) {
				addPageItem('ellipsis-h', 0, true, false);
			}

			addPageItem(
				pageNumber,
				pageNumber,
				false,
				pageNumber === currentPageNumber
			);

			previousPageNumberShown = pageNumber;
		}

		addPageItem(
			'angle-right',
			currentPageNumber + 1,
			currentPageNumber === pagesCount,
			false
		);
	};

	pageSizeSelect.addEventListener('change', () => {
		currentPageNumber = 1;

		showPage();
	});

	showPage();
}
