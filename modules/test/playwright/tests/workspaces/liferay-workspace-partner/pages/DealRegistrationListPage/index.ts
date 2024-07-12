/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Locator, Page} from '@playwright/test';

import {
	DealRegistrationForm,
	DealRegistrationFormContent,
} from './DealRegistrationFormPage/Forms/dealRegistrationForm';

type FormContent = {
	generals: DealRegistrationFormContent;
};

export class DealRegistrationFormPage {
	readonly form: {
		generals: DealRegistrationForm;
		review: {};
	};
	readonly page: Page;
	readonly proceedButton: Locator;

	constructor(page: Page) {
		this.form = {
			generals: new DealRegistrationForm(page),
			review: {},
		};
		this.page = page;
		this.proceedButton = page.getByRole('button', {name: 'Proceed'});
	}

	async createNewDealRegistration(form: FormContent) {
		await this.form.generals.fillForm(form.generals);
		await this.proceedButton.click();
	}
}
