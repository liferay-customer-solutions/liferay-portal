/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, lazy} from 'react';
import i18n from '~/i18n';
import {AppRoute} from '~/utils/routeUtils';
import type {DeliveryProduct} from '~/types/product';
import {getSpecificationValue} from '~/hooks/useProjectCommerce';

const AccountSelection = lazy(
	() => import('./AccountSelection/AccountSelection')
);
const ActivationKeyForm = lazy(
	() => import('./ActivationKeyForm/ActivationKeyForm')
);
const LDPProvisioning = lazy(() => import('./LDPProvisioning/LDPProvisioning'));
const License = lazy(() => import('./License/License'));
const PaymentMethod = lazy(() => import('./PaymentMethod/PaymentMethod'));
const Summary = lazy(() => import('./Summary/Summary'));

// AI Hub Pages

const AIHubForm = lazy(() => import('./pages/LiferayProduct/AIHub/AIHubForm'));
const AIHubOpenBetaForm = lazy(
	() => import('./pages/LiferayProduct/AIHub/AIHubOpenBetaForm')
);
const AIHubOrderSummary = lazy(
	() => import('./pages/LiferayProduct/AIHub/AIHubOrderSummary')
);
const AIHubPaymentMethod = lazy(
	() => import('./pages/LiferayProduct/AIHub/AIHubPaymentMethod')
);
const AIHubTokenOrderSummary = lazy(
	() => import('./pages/LiferayProduct/AIHub/AIHubTokenOrderSummary')
);
const AIHubTokenSelection = lazy(
	() => import('./pages/LiferayProduct/AIHub/AIHubTokenSelection')
);
const ProjectSelection = lazy(() => import('./pages/LiferayProduct/Project'));

export type ProductPurchaseStep = {
	element: ReactNode;
	excludeForDXPFree?: boolean;
	index?: boolean;
	isDXPFreeOnly?: boolean;
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
	isLDP = false,
	isPaidApp,
	product,
	searchParams = new URLSearchParams(),
}: {
	isLDP?: boolean;
	isPaidApp: boolean;
	product?: DeliveryProduct;
	searchParams?: URLSearchParams;
}): ProductPurchaseStep[] {
	if (product) {
		const solutionType = getSpecificationValue(product, 'solution-type');

		if (solutionType === 'ai-hub') {
			return [
				{
					element: <AccountSelection />,
					index: true,
					title: i18n.translate('account'),
				},
				{
					element: <AIHubForm />,
					path: 'ai-hub-form',
					title: i18n.translate('ai-hub'),
				},
			];
		}

		if (solutionType === 'ai-hub-open-beta') {
			if (searchParams.has('aiHubTokens')) {
				return [
					{
						element: <AIHubTokenSelection />,
						index: true,
						title: i18n.translate('tokens-amount'),
					},
					{
						element: <AIHubPaymentMethod />,
						path: 'payment-method',
						title: i18n.translate('payment-method'),
					},
					{
						element: <AIHubTokenOrderSummary />,
						path: 'summary',
						title: i18n.translate('summary'),
					},
				];
			}

			return [
				{
					element: <AccountSelection />,
					index: true,
					title: i18n.translate('account'),
				},
				{
					element: <ProjectSelection />,
					path: 'project',
					title: i18n.translate('project'),
				},
				{
					element: <AIHubOpenBetaForm />,
					path: 'ai-hub-open-beta-form',
					title: i18n.translate('account-details'),
				},
				{
					element: <AIHubOrderSummary />,
					path: 'summary',
					title: i18n.translate('summary'),
				},
			];
		}
	}
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
			element: <ActivationKeyForm />,
			isDXPFreeOnly: true,
			path: 'activation-key-form',
			title: i18n.translate('activation-key'),
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
			excludeForDXPFree: true,
			path: 'summary',
			title: i18n.translate('summary'),
		},
	];

	return steps.filter(
		(step) =>
			(isPaidApp || !step.isPaidOnly) &&
			(isLDP || !step.isLDPOnly) &&
			(isDXPFree || !step.isDXPFreeOnly) &&
			(!isDXPFree || !step.excludeForDXPFree)
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
