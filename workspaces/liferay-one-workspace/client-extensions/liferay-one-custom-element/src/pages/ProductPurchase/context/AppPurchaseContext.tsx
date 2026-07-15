/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {createContext, useContext, useMemo, useReducer} from 'react';

import {PaymentMethodType} from '../types';

import type {ConsoleUserProject} from '~/services/spring-boot/types';
import type {BillingAddress} from '~/types/orders';
import type {SalesforceProject} from '~/types/salesforceProject';

export type LicenseType = 'TRIAL' | 'PAID';

type Invoice = {
	email: string;
	purchaseOrderNumber: string;
};

type Payment = {
	billingAddress: BillingAddress;
	eulaAgreement: boolean;
	invoice: Invoice;
	taxId: string;
	type: PaymentMethodType;
};

type AppPurchaseState = {
	licenseType: LicenseType;
	payment: Payment;
	project: ConsoleUserProject;
	salesforceProject: SalesforceProject | null;
};

type AppPurchaseAction =
	| {taxId: string; type: 'setAccountTaxId'}
	| {billingAddress: Partial<BillingAddress>; type: 'setBillingAddress'}
	| {invoice: Invoice; type: 'setInvoice'}
	| {licenseType: LicenseType; type: 'setLicenseType'}
	| {paymentMethodType: PaymentMethodType; type: 'setPaymentMethodType'}
	| {project: ConsoleUserProject; type: 'setProject'}
	| {salesforceProject: SalesforceProject; type: 'setSalesforceProject'}
	| {type: 'toggleEulaAgreement'};

const initialState: AppPurchaseState = {
	licenseType: 'TRIAL',
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
		},
		taxId: '',
		type: PaymentMethodType.PAY_NOW,
	},
	project: null as unknown as ConsoleUserProject,
	salesforceProject: null,
};

function appPurchaseReducer(
	state: AppPurchaseState,
	action: AppPurchaseAction
): AppPurchaseState {
	switch (action.type) {
		case 'setAccountTaxId':
			return {
				...state,
				payment: {...state.payment, taxId: action.taxId},
			};
		case 'setBillingAddress':
			return {
				...state,
				payment: {
					...state.payment,
					billingAddress: action.billingAddress as BillingAddress,
				},
			};
		case 'setInvoice':
			return {
				...state,
				payment: {...state.payment, invoice: action.invoice},
			};
		case 'setLicenseType':
			return {
				...state,
				licenseType: action.licenseType,
				payment:
					action.licenseType === 'PAID'
						? {...state.payment, type: PaymentMethodType.PAY_NOW}
						: state.payment,
			};
		case 'setPaymentMethodType':
			return {
				...state,
				payment: {...state.payment, type: action.paymentMethodType},
			};
		case 'setProject':
			return {...state, project: action.project};
		case 'setSalesforceProject':
			return {...state, salesforceProject: action.salesforceProject};
		case 'toggleEulaAgreement':
			return {
				...state,
				payment: {
					...state.payment,
					eulaAgreement: !state.payment.eulaAgreement,
				},
			};
		default:
			return state;
	}
}

type AppPurchaseContextValue = AppPurchaseState & {
	setAccountTaxId: (taxId: string) => void;
	setBillingAddress: (billingAddress: Partial<BillingAddress>) => void;
	setInvoice: (invoice: Invoice) => void;
	setLicenseType: (licenseType: LicenseType) => void;
	setPaymentMethodType: (paymentMethodType: PaymentMethodType) => void;
	setProject: (project: ConsoleUserProject) => void;
	setSalesforceProject: (salesforceProject: SalesforceProject) => void;
	toggleEulaAgreement: () => void;
};

const AppPurchaseContext = createContext<AppPurchaseContextValue | null>(null);

export function AppPurchaseProvider({children}: {children: React.ReactNode}) {
	const [state, dispatch] = useReducer(appPurchaseReducer, initialState);

	const value = useMemo<AppPurchaseContextValue>(
		() => ({
			...state,
			setAccountTaxId: (taxId) =>
				dispatch({taxId, type: 'setAccountTaxId'}),
			setBillingAddress: (billingAddress) =>
				dispatch({billingAddress, type: 'setBillingAddress'}),
			setInvoice: (invoice) => dispatch({invoice, type: 'setInvoice'}),
			setLicenseType: (licenseType) =>
				dispatch({licenseType, type: 'setLicenseType'}),
			setPaymentMethodType: (paymentMethodType) =>
				dispatch({paymentMethodType, type: 'setPaymentMethodType'}),
			setProject: (project) => dispatch({project, type: 'setProject'}),
			setSalesforceProject: (salesforceProject) =>
				dispatch({salesforceProject, type: 'setSalesforceProject'}),
			toggleEulaAgreement: () => dispatch({type: 'toggleEulaAgreement'}),
		}),
		[state]
	);

	return (
		<AppPurchaseContext.Provider value={value}>
			{children}
		</AppPurchaseContext.Provider>
	);
}

export function useAppPurchaseContext() {
	const context = useContext(AppPurchaseContext);

	if (!context) {
		throw new Error(
			'useAppPurchaseContext must be used within AppPurchaseProvider'
		);
	}

	return context;
}
