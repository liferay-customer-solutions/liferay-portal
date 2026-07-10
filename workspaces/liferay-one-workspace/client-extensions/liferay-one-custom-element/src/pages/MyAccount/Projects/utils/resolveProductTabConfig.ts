/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {getSpecificationValue} from '~/hooks/useProjectCommerce';

import {PROJECT_TAB_ORDER, SUPPORT_SPECIFICATION_KEYS} from './constants';
import {resolveActivationProfile} from './resolveActivationProfile';
import {resolveDetailsProfile} from './resolveDetailsProfile';
import {resolveDownloadProfile} from './resolveDownloadProfile';
import {resolveEnvironmentProfile} from './resolveEnvironmentProfile';
import {resolveUtilizationProfile} from './resolveUtilizationProfile';

import type {DeliveryProduct} from '~/types/product';

import type {ProjectItemKind, ProjectTabKey} from '../types';
import type {ActivationProfile} from './resolveActivationProfile';
import type {DetailsProfile} from './resolveDetailsProfile';
import type {DownloadProfile} from './resolveDownloadProfile';
import type {EnvironmentProfile} from './resolveEnvironmentProfile';
import type {UtilizationProfile} from './resolveUtilizationProfile';

export type ProductTabConfig = {
	activationProfile?: ActivationProfile;
	detailsProfile: DetailsProfile;
	downloadProfile?: DownloadProfile;
	environmentProfile?: EnvironmentProfile;
	learnUrl?: string;
	tabKeys: ProjectTabKey[];
	utilizationProfile?: UtilizationProfile;
};

export function resolveProductTabConfig({
	kind,
	orderType,
	product,
}: {
	kind: ProjectItemKind;
	orderType?: string;
	product: DeliveryProduct;
}): ProductTabConfig {
	const activationProfile = resolveActivationProfile({
		kind,
		orderType,
		product,
	});

	const detailsProfile = resolveDetailsProfile({kind, product});
	const downloadProfile = resolveDownloadProfile(product);
	const environmentProfile = resolveEnvironmentProfile(product);
	const utilizationProfile = resolveUtilizationProfile(product);
	const learnUrl = getSpecificationValue(product, 'project-learn-url');

	const hasSupportInfo =
		Boolean(learnUrl) ||
		SUPPORT_SPECIFICATION_KEYS.some((specificationKey) =>
			getSpecificationValue(product, specificationKey)
		);

	const tabPresent: Record<ProjectTabKey, boolean> = {
		'activation': activationProfile !== 'none',
		'details': true,
		'download': downloadProfile !== 'none',
		'environment': environmentProfile !== 'none',
		'help-and-support': hasSupportInfo,
		'orders': true,
		'utilization': utilizationProfile !== 'none',
	};

	return {
		...(activationProfile !== 'none' && {activationProfile}),
		detailsProfile,
		...(downloadProfile !== 'none' && {downloadProfile}),
		...(environmentProfile !== 'none' && {environmentProfile}),
		...(learnUrl && {learnUrl}),
		tabKeys: PROJECT_TAB_ORDER.filter((tabKey) => tabPresent[tabKey]),
		...(utilizationProfile !== 'none' && {utilizationProfile}),
	};
}

export default resolveProductTabConfig;
