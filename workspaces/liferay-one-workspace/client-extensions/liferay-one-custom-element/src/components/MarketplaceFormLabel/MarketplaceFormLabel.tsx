/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {ClayTooltipProvider} from '@clayui/tooltip';
import classNames from 'classnames';
import {LabelHTMLAttributes} from 'react';

import '~/components/MarketplaceForm/MarketplaceForm.scss';

interface ILabelProps extends LabelHTMLAttributes<HTMLLabelElement> {
	info?: string;
	required?: boolean;
}

export function MarketplaceFormLabel({
	className,
	info,
	required,
	...props
}: ILabelProps) {
	return (
		<div
			className={classNames(
				'align-items-center d-flex marketplace-form-label',
				className
			)}
		>
			<label {...props} className="mb-0 w-auto" />

			{required && (
				<ClayIcon
					className="required-icon text-danger"
					symbol="asterisk"
				/>
			)}

			{info && (
				<ClayTooltipProvider>
					<div
						className="info-bg inline-item"
						data-tooltip-align="top"
						title={info}
					>
						<ClayIcon
							className="info-icon"
							symbol="info-circle-open"
						/>
					</div>
				</ClayTooltipProvider>
			)}
		</div>
	);
}
