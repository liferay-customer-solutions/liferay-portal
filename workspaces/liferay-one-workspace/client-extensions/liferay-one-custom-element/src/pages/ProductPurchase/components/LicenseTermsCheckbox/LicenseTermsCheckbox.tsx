/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayCheckbox} from '@clayui/form';
import i18n from '~/i18n';
import {
	ProductSpecificationKey,
	getProductSpecificationValue,
} from '~/utils/productUtils';
import {getSiteName} from '~/utils/siteUtils';

import type {DeliveryProduct} from '~/types/product';

const MARKETPLACE_TERMS_OF_SERVICE_URL =
	'https://www.liferay.com/legal/marketplace-terms-of-service';

type LicenseTermsCheckboxProps = {
	checked: boolean;
	onChange: () => void;
	product: DeliveryProduct;
};

const LicenseTermsCheckbox = ({
	checked,
	onChange,
	product,
}: LicenseTermsCheckboxProps) => {
	const appUsageTermsURL = getProductSpecificationValue(
		ProductSpecificationKey.APP_SUPPORT_USAGE_TERMS_URL,
		product
	);

	let eulaURL = `/documents/d/${getSiteName()}/end_user_license_agreement`;

	if (appUsageTermsURL) {
		eulaURL = appUsageTermsURL.startsWith('https://')
			? appUsageTermsURL
			: `https://${appUsageTermsURL}`;
	}

	return (
		<div className="align-items-start d-flex mt-4">
			<ClayCheckbox
				checked={checked}
				className="mr-2"
				onChange={onChange}
			/>

			<span className="ml-1">
				{i18n.translate('i-have-read-and-agree-to-the')}{' '}
				<a href={eulaURL} rel="noopener noreferrer" target="_blank">
					{i18n.translate('end-user-license-agreement')}
				</a>{' '}
				{i18n.translate('and-the')}{' '}
				<a
					href={MARKETPLACE_TERMS_OF_SERVICE_URL}
					rel="noopener noreferrer"
					target="_blank"
				>
					{i18n.translate('terms')}
				</a>{' '}
				{i18n.translate('of-service')}
			</span>
		</div>
	);
};

export default LicenseTermsCheckbox;
