/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n, {Word} from '~/i18n';

import DetailsCard from '../DetailsCard/DetailsCard';

import type {DeliveryProductSpecification} from '~/types/product';

type HelpSupportCardProps = {
	specifications: DeliveryProductSpecification[];
};

type SupportLink = {
	href: (value: string) => string;
	label: Word;
	specificationKey: string;
};

function withProtocol(value: string): string {
	return /^https?:\/\//.test(value) ? value : `https://${value}`;
}

const SUPPORT_LINKS: SupportLink[] = [
	{
		href: withProtocol,
		label: 'support-url',
		specificationKey: 'support-url',
	},
	{
		href: withProtocol,
		label: 'publisher-website',
		specificationKey: 'publisher-web-site-url',
	},
	{
		href: (value) => `mailto:${value}`,
		label: 'support-email-address',
		specificationKey: 'support-email-address',
	},
	{
		href: (value) => `tel:${value.replace(/\s/g, '')}`,
		label: 'support-phone-number',
		specificationKey: 'support-phone',
	},
	{
		href: withProtocol,
		label: 'app-usage-terms-eula-url',
		specificationKey: 'app-usage-terms-url',
	},
	{
		href: withProtocol,
		label: 'app-documentation-url',
		specificationKey: 'app-documentation-url',
	},
];

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
