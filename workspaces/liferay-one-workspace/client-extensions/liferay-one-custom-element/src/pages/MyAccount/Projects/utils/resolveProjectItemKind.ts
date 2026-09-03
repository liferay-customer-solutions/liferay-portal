/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {resolveProfile} from './resolveProfile';

import type {DeliveryProductSpecification} from '~/types/product';

import type {ProjectItemKind} from '../types';

const PROJECT_ITEM_KINDS: ProjectItemKind[] = ['application', 'product'];

export function resolveProjectItemKind(
	specifications: DeliveryProductSpecification[]
): ProjectItemKind {
	const specification = specifications.find(
		({specificationKey}) => specificationKey === 'project-item-kind'
	);

	return resolveProfile(
		(specification?.value ?? '').toLowerCase(),
		PROJECT_ITEM_KINDS,
		'product'
	);
}

export default resolveProjectItemKind;
