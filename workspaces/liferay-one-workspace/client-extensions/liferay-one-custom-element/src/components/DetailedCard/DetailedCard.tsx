/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {ReactNode} from 'react';
import CustomIcon from '~/components/CustomIcon/CustomIcon';

import './DetailedCard.css';

type DetailedCardProps = {
	cardIcon?: string;
	cardIconAltText: string;
	cardTitle: string;
	children: ReactNode;
	className?: string;
	clayIcon?: string;
	clayIconSpritemap?: string;
	fitContent?: boolean;
	headerActions?: ReactNode;
	iconPosition?: 'left' | 'right';
	sizing?: 'lg';
};

export function DetailedCard({
	cardIcon,
	cardIconAltText,
	cardTitle,
	children,
	className,
	clayIcon,
	clayIconSpritemap,
	fitContent,
	headerActions,
	iconPosition = 'right',
}: DetailedCardProps) {
	const icon = (
		<div
			className={classNames(
				'align-items-center d-flex detailed-card-header-icon-container justify-content-center',
				iconPosition === 'left' ? 'mr-3' : 'ml-3'
			)}
		>
			{clayIcon ? (
				<CustomIcon
					className="detailed-card-header-clay-icon"
					spritemap={clayIconSpritemap}
					symbol={clayIcon}
				/>
			) : (
				<img alt={cardIconAltText} src={cardIcon} />
			)}
		</div>
	);

	return (
		<div
			className={
				[className, fitContent && 'detailed-card-fit']
					.filter(Boolean)
					.join(' ') || undefined
			}
		>
			<div className="detailed-card-container">
				<div className="align-items-center d-flex detailed-card-header flex-row justify-content-between pr-2">
					{iconPosition === 'left' && icon}

					<h2>{cardTitle}</h2>

					{headerActions && (
						<div className="align-items-center d-flex ml-auto mr-4">
							{headerActions}
						</div>
					)}

					{iconPosition === 'right' && icon}
				</div>

				{children}
			</div>
		</div>
	);
}
