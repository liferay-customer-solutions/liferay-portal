/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayRadio} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {ReactNode} from 'react';

import './RadioCard.css';

type RadioCardProps = {
	className?: string;
	content?: ReactNode;
	description?: string;
	disabled?: boolean;
	icon?: string;
	onChange: () => void;
	selected: boolean;
	title?: string;
	tooltip?: string;
};

const RadioCard = ({
	className,
	content,
	description,
	disabled,
	icon,
	onChange,
	selected,
	title,
	tooltip,
}: RadioCardProps) => (
	<div
		className={classNames(
			'border p-3 product-purchase-radio-card rounded',
			{disabled, selected},
			className
		)}
		onClick={disabled ? undefined : onChange}
		role="button"
		tabIndex={disabled ? -1 : 0}
		title={tooltip}
	>
		<div className="radio-card-control">
			<ClayRadio
				checked={selected}
				disabled={disabled}
				onChange={onChange}
				value=""
			/>

			{content || (
				<div className="align-items-center d-flex">
					{icon && <ClayIcon className="mr-2" symbol={icon} />}

					<div>
						<strong className="d-block">{title}</strong>

						{description && (
							<small className="text-muted">{description}</small>
						)}
					</div>
				</div>
			)}
		</div>
	</div>
);

export {RadioCard};
export default RadioCard;
