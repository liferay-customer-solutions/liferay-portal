/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {createStore} from '@xstate/store';

import {ConsoleUserProject} from '~/services/spring-boot/types';
import {SalesforceProject} from '~/types/salesforceProject';
import {PaymentMethodType} from '../types';
import type {BillingAddress} from '~/types/orders';

export type LicenseType = 'TRIAL' | 'PAID';

type Invoice = {
	email: string;
	purchaseOrderNumber: string;
};

type AppPurchaseContext = {
	licenseType: LicenseType;
	payment: {
		billingAddress: BillingAddress;
		eulaAgreement: boolean;
		invoice: Invoice;
		taxId: string;
		type: PaymentMethodType;
	};
	project: ConsoleUserProject;
	salesforceProject: SalesforceProject | null;
};

export const productPurchaseStore = createStore({
	context: {
		licenseType: 'TRIAL' as LicenseType,
		payment: {
			billingAddress: {
				city: '',
				country: '',
				countryISOCode: '',
				name: '',
				phoneNumber: '',
				regionISOCode: '',
				street1: '',
				street2: '',
				zip: '',
			} as BillingAddress,
			eulaAgreement: false,
			invoice: {
				email: '',
				purchaseOrderNumber: '',
			} as Invoice,
			taxId: '',
			type: PaymentMethodType.PAY_NOW,
		},
		project: null as unknown as ConsoleUserProject,
		salesforceProject: null as SalesforceProject | null,
	} as AppPurchaseContext,
	on: {
		setAccountTaxId: {
			payment: (context: AppPurchaseContext, event: {taxId: string}) => ({
				...context.payment,
				taxId: event.taxId,
			}),
		},
		setBillingAddress: {
			payment: (
				context: AppPurchaseContext,
				event: {billingAddress: Partial<BillingAddress>}
			) => ({
				...context.payment,
				billingAddress: event.billingAddress as BillingAddress,
			}),
		},

		setInvoice: {
			payment: (
				context: AppPurchaseContext,
				event: {invoice: Invoice}
			) => ({
				...context.payment,
				invoice: event.invoice,
			}),
		},

		setLicenseType: {
			licenseType: (
				context: AppPurchaseContext,
				event: {licenseType: LicenseType}
			) => {
				if (event.licenseType === 'PAID') {
					context.payment.type = PaymentMethodType.PAY_NOW;
				}

				return event.licenseType;
			},
		},

		setPaymentMethodType: {
			payment: (
				context: AppPurchaseContext,
				event: {paymentMethodType: PaymentMethodType}
			) => ({
				...context.payment,
				type: event.paymentMethodType,
			}),
		},

		setProject: {
			project: (
				_: AppPurchaseContext,
				event: {project: ConsoleUserProject}
			) => event.project,
		},

		setSalesforceProject: {
			salesforceProject: (
				_: AppPurchaseContext,
				event: {salesforceProject: SalesforceProject}
			) => event.salesforceProject,
		},

		toggleEulaAgreement: {
			payment: (context: AppPurchaseContext) => {
				context.payment.eulaAgreement = !context.payment.eulaAgreement;

				return context.payment;
			},
		},
	},
});
