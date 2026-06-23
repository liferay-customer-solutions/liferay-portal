/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import type {DeliveryProductSpecification} from '~/types/product';

export function getValueFromDeliverySpecifications(
	specifications: DeliveryProductSpecification[],
	valueKey: string
) {
	let value = '';
	specifications?.forEach((specification) => {
		if (specification?.specificationKey === valueKey) {
			value = specification?.value;
		}
	});

	return value;
}

export default getValueFromDeliverySpecifications;
