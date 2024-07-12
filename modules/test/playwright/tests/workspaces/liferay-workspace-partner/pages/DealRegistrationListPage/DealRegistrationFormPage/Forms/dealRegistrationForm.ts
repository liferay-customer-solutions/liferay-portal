/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Locator, Page} from '@playwright/test';

export type DealRegistrationFormContent = {
	accountName: string;
	address: string;
	businessUnit: string;
	city: string;
	country: string;
	department: string;
	emailAddress: string;
	firstName: string;
	jobRole: string;
	lastName: string;
	partnerAccount: string;
	phone: string;
	postalCode: string;
	projectTimeline: string;
	prospectIndustry: string;
	title: string;
};

export class DealRegistrationForm {
	readonly accountName: Locator;
	readonly address: Locator;
	readonly businessUnit: Locator;
	readonly city: Locator;
	readonly country: Locator;
	readonly department: Locator;
	readonly emailAddress: Locator;
	readonly firstName: Locator;
	readonly jobRole: Locator;
	readonly lastName: Locator;
	readonly page: Page;
	readonly partnerAccount: Locator;
	readonly phone: Locator;
	readonly postalCode: Locator;
	readonly projectNeed: {
		customerPortal: Locator;
	};
	readonly projectSolutionCategories: {
		B2B2C: Locator;
	};
	readonly projectTimeline: Locator;
	readonly prospectIndustry: Locator;
	readonly title: Locator;

	constructor(page: Page) {
		this.accountName = page.locator('input[name="prospect\\.accountName"]');
		this.address = page.locator('input[name="prospect\\.address"]');
		this.businessUnit = page.locator(
			'input[name="primaryProspect\\.businessUnit"]'
		);
		this.city = page.locator('input[name="prospect\\.city"]');
		this.country = page.locator('select[name="prospect\\.country"]');
		this.department = page.locator(
			'select[name="primaryProspect\\.department"]'
		);
		this.emailAddress = page.locator(
			'input[name="primaryProspect\\.emailAddress"]'
		);
		this.firstName = page.locator(
			'input[name="primaryProspect\\.firstName"]'
		);
		this.jobRole = page.locator('select[name="primaryProspect\\.jobRole"]');
		this.lastName = page.locator(
			'input[name="primaryProspect\\.lastName"]'
		);
		this.page = page;
		this.partnerAccount = page.locator('select[name="partnerAccount"]');
		this.phone = page.locator('input[name="primaryProspect\\.phone"]');
		this.postalCode = page.locator('input[name="prospect\\.postalCode"]');
		this.projectNeed = {
			customerPortal: page.getByLabel('Customer Portal'),
		};
		this.projectSolutionCategories = {B2B2C: page.getByLabel('B2B2C')};
		this.projectTimeline = page.locator('input[name="projectTimeline"]');
		this.prospectIndustry = page.locator(
			'select[name="prospect\\.industry"]'
		);
		this.title = page.locator('input[name="primaryProspect\\.title"]');
	}

	async selectCountry(country: string) {
		await this.country.selectOption({label: country});
	}

	async selectDepartment(department: string) {
		await this.department.selectOption({label: department});
	}

	async selectJobRole(jobRole: string) {
		await this.jobRole.selectOption({label: jobRole});
	}

	async selectPartnerAccount(partnerAccount: string) {
		await this.partnerAccount.selectOption({label: partnerAccount});
	}

	async selectProspectIndustry(prospectIndustry: string) {
		await this.prospectIndustry.selectOption({label: prospectIndustry});
	}

	async fillForm({
		accountName,
		address,
		businessUnit,
		city,
		country,
		department,
		emailAddress,
		firstName,
		jobRole,
		lastName,
		partnerAccount,
		phone,
		postalCode,
		projectTimeline,
		prospectIndustry,
		title,
	}: DealRegistrationFormContent) {
		await this.accountName.fill(accountName);
		await this.address.fill(address);
		await this.businessUnit.fill(businessUnit);
		await this.city.fill(city);
		await this.emailAddress.fill(emailAddress);
		await this.firstName.fill(firstName);
		await this.selectJobRole(jobRole);
		await this.lastName.fill(lastName);
		await this.phone.fill(phone);
		await this.postalCode.fill(postalCode);
		await this.projectNeed.customerPortal.check();
		await this.projectSolutionCategories.B2B2C.check();
		await this.projectTimeline.fill(projectTimeline);
		await this.selectCountry(country);
		await this.selectDepartment(department);
		await this.selectPartnerAccount(partnerAccount);
		await this.selectProspectIndustry(prospectIndustry);
		await this.title.fill(title);
	}
}
