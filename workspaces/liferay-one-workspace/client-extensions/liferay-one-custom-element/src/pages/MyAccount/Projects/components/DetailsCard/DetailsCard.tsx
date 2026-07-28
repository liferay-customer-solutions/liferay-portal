/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n, {Word} from '~/i18n';

export type DetailsRow = {
	label: string;
	value: ReactNode;
};

type DetailsCardProps = {
	bodyClassName?: string;
	fullWidth?: boolean;
	headerActions?: ReactNode;
	icon?: string;
	iconPosition?: 'left' | 'right';
	iconSpritemap?: string;
	rows: DetailsRow[];
	title?: Word;
};

export default function DetailsCard({
	bodyClassName = 'mt-3',
	fullWidth = false,
	headerActions,
	icon = 'catalog',
	iconPosition = 'right',
	iconSpritemap,
	rows,
	title = 'details',
}: DetailsCardProps) {
	return (
		<DetailedCard
			cardIconAltText={i18n.translate(title)}
			cardTitle={i18n.translate(title)}
			className="mt-3"
			clayIcon={icon}
			clayIconSpritemap={iconSpritemap}
			fitContent={!fullWidth}
			headerActions={headerActions}
			iconPosition={iconPosition}
		>
			<div
				className={bodyClassName}
				style={
					fullWidth
						? {
								columnGap: 'var(--spacer-5)',
								display: 'grid',
								gridTemplateColumns:
									'repeat(auto-fill, minmax(20rem, 1fr))',
								rowGap: 'var(--spacer-3)',
							}
						: {
								display: 'flex',
								flexDirection: 'column',
								gap: 'var(--spacer-3)',
								maxWidth: '32rem',
							}
				}
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
