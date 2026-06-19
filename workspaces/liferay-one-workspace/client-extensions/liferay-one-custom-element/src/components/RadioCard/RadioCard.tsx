/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayRadio} from '@clayui/form';
import classNames from 'classnames';
import {ReactNode} from 'react';

type RadioCardProps = {
	className?: string;
	content?: ReactNode;
	description?: string;
	onChange: () => void;
	selected: boolean;
	title?: string;
};

const RadioCard = ({
	className,
	content,
	description,
	onChange,
	selected,
	title,
}: RadioCardProps) => (
	<div
		className={classNames(
			'border p-3 product-purchase-radio-card rounded',
			{selected},
			className
		)}
		onClick={onChange}
		role="button"
		tabIndex={0}
	>
		<div className="align-items-center d-flex">
			<ClayRadio
				checked={selected}
				className="mr-2"
				onChange={onChange}
				value=""
			/>

			{content || (
				<div>
					<strong className="d-block">{title}</strong>

					{description && (
						<small className="text-muted">{description}</small>
					)}
				</div>
			)}
		</div>
	</div>
);

export default RadioCard;
