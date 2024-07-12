/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {mergeTests} from '@playwright/test';

import {apiHelpersTest} from '../../../../fixtures/apiHelpersTest';
import {partnerHelper} from '../fixtures/partnerHelper';
import {partnerPagesTest} from '../fixtures/partnerPages';
import {partnerSiteFixture} from '../fixtures/partnerSite';
import {generateDealRegistrationData} from '../utils/dealRegistrationData';

const test = mergeTests(
	apiHelpersTest,
	partnerHelper,
	partnerSiteFixture,
	partnerPagesTest
);

test.describe('Deal Registration Form', () => {
	test('Should Create a New Deal Registration', async ({
		page,
		partnerDealRegistrationForm,
	}) => {
		await page.goto('/web/liferay-partner/sales/deal-registrations/new');

		const dealRegistrationData = generateDealRegistrationData();

		await partnerDealRegistrationForm.createNewDealRegistration(
			dealRegistrationData
		);
	});
});
