/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n, {Word} from '~/i18n';

import {DetailsRow} from '../DetailsCard/DetailsCard';

import './SectionedDetailsCard.css';

export type DetailsSection = {
	rows: DetailsRow[];
	title?: string;
};

type SectionedDetailsCardProps = {
	icon?: string;
	sections: DetailsSection[];
	title?: Word;
};

export default function SectionedDetailsCard({
	icon = 'catalog',
	sections,
	title = 'details',
}: SectionedDetailsCardProps) {
	return (
		<DetailedCard
			cardIconAltText={i18n.translate(title)}
			cardTitle={i18n.translate(title)}
			className="mt-3"
			clayIcon={icon}
		>
			<div className="sectioned-details-card">
				{sections.map((section, index) => (
					<div
						className={
							section.title
								? 'sectioned-details-card-section'
								: 'sectioned-details-card-section sectioned-details-card-section--plain'
						}
						key={section.title ?? String(index)}
					>
						{section.title && (
							<span className="sectioned-details-card-section-title">
								{section.title}
							</span>
						)}

						{section.rows.map((row) => (
							<div
								className="sectioned-details-card-row"
								key={row.label}
							>
								<span className="sectioned-details-card-row-label">
									{row.label}
								</span>

								<span className="sectioned-details-card-row-value">
									{row.value}
								</span>
							</div>
						))}
					</div>
				))}
			</div>
		</DetailedCard>
	);
}
