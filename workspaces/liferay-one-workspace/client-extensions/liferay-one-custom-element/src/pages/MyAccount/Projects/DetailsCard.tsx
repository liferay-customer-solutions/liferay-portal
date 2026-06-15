/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import {DetailedCard} from '../../../components/DetailedCard/DetailedCard';
import i18n, {Word} from '../../../i18n';

export type DetailsRow = {
	label: string;
	value: ReactNode;
};

type DetailsCardProps = {
	icon?: string;
	rows: DetailsRow[];
	title?: Word;
};

export default function DetailsCard({
	icon = 'catalog',
	rows,
	title = 'details',
}: DetailsCardProps) {
	return (
		<DetailedCard
			cardIconAltText={i18n.translate(title)}
			cardTitle={i18n.translate(title)}
			className="mt-3"
			clayIcon={icon}
		>
			<div
				className="d-flex flex-column mt-3"
				style={{gap: 'var(--spacer-3)', maxWidth: '32rem'}}
			>
				{rows.map((row) => (
					<div
						className="align-items-baseline d-flex"
						key={row.label}
					>
						<span
							style={{
								color: 'var(--color-neutral-10)',
								flex: '0 0 45%',
								fontWeight: 600,
							}}
						>
							{row.label}
						</span>

						<span style={{color: 'var(--color-neutral-8)'}}>
							{row.value}
						</span>
					</div>
				))}
			</div>
		</DetailedCard>
	);
}
