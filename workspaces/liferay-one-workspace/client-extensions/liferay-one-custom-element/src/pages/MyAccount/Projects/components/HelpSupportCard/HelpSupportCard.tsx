/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import {SUPPORT_LINKS} from '../../utils/constants';
import DetailsCard from '../DetailsCard/DetailsCard';

import type {DeliveryProductSpecification} from '~/types/product';

type HelpSupportCardProps = {
	specifications: DeliveryProductSpecification[];
};

export default function HelpSupportCard({
	specifications,
}: HelpSupportCardProps) {
	const rows = SUPPORT_LINKS.map((link) => {
		const value = specifications.find(
			(specification) =>
				specification.specificationKey === link.specificationKey
		)?.value;

		if (!value) {
			return null;
		}

		return {
			label: i18n.translate(link.label),
			value: (
				<a
					href={link.href(value)}
					rel="noopener noreferrer"
					target="_blank"
				>
					{value}
				</a>
			),
		};
	}).filter((row): row is {label: string; value: JSX.Element} =>
		Boolean(row)
	);

	return (
		<DetailsCard
			icon="question-circle"
			rows={rows}
			title="help-and-support"
		/>
	);
}
