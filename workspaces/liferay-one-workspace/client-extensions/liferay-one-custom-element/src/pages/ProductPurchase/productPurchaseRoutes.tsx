/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, lazy} from 'react';
import i18n from '~/i18n';
import {AppRoute} from '~/utils/routeUtils';

const AccountSelection = lazy(
	() => import('./AccountSelection/AccountSelection')
);
const LDPProvisioning = lazy(
	() => import('./LDPProvisioning/LDPProvisioning')
);
const License = lazy(() => import('./License/License'));
const PaymentMethod = lazy(() => import('./PaymentMethod/PaymentMethod'));
const Summary = lazy(() => import('./Summary/Summary'));

export type ProductPurchaseStep = {
	element: ReactNode;
	index?: boolean;
	isLDPOnly?: boolean;
	isPaidOnly?: boolean;
	path?: string;
	title: string;
};

export type ProductPurchaseStepItem = {
	key: string;
	title: string;
};

export function getProductPurchaseSteps({
	isLDP,
	isPaidApp,
}: {
	isLDP: boolean;
	isPaidApp: boolean;
}): ProductPurchaseStep[] {
	const steps: ProductPurchaseStep[] = [
		{
			element: <AccountSelection />,
			index: true,
			title: i18n.translate('account'),
		},
		{
			element: <License />,
			isPaidOnly: true,
			path: 'license',
			title: i18n.translate('license-selection'),
		},
		{
			element: <PaymentMethod />,
			isPaidOnly: true,
			path: 'payment-method',
			title: i18n.translate('payment-method'),
		},
		{
			element: <LDPProvisioning />,
			isLDPOnly: true,
			path: 'provisioning',
			title: i18n.translate('provisioning'),
		},
		{
			element: <Summary />,
			path: 'summary',
			title: i18n.translate('summary'),
		},
	];

	return steps.filter(
		(step) => (isPaidApp || !step.isPaidOnly) && (isLDP || !step.isLDPOnly)
	);
}

export function getStepKey(step: Pick<ProductPurchaseStep, 'index' | 'path'>) {
	return step.index ? '/' : `/${step.path}`;
}

export function toStepRoutes(steps: ProductPurchaseStep[]): AppRoute[] {
	return steps.map((step) =>
		step.index
			? {element: step.element, index: true}
			: {element: step.element, path: step.path as string}
	);
}

export function toStepItems(
	steps: ProductPurchaseStep[]
): ProductPurchaseStepItem[] {
	return steps.map((step) => ({key: getStepKey(step), title: step.title}));
}
