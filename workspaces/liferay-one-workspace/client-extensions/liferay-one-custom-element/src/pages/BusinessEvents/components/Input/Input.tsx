/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm, {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {ReactNode} from 'react';
import Badge from '~/components/Badge/Badge';

import './Input.css';

interface IProps extends React.ComponentPropsWithoutRef<typeof ClayInput> {
	badgeClassName?: string;
	disableError?: boolean;
	error?: string;
	groupStyle?: string;
	helper?: ReactNode;
	label: string;
}

const Input = ({
	badgeClassName,
	disableError,
	error,
	groupStyle,
	helper,
	label,
	...props
}: IProps) => {
	return (
		<ClayForm.Group
			className={classNames('w-100', {
				groupStyle,
				'has-error': Boolean(error),
			})}
		>
			<label>
				{`${label} `}

				{props.required && (
					<span className="inline-item-after reference-mark text-warning">
						<ClayIcon symbol="asterisk" />
					</span>
				)}

				<ClayInput {...props} />
			</label>

			{error && !disableError ? (
				<Badge badgeClassName={badgeClassName}>
					<span className="pl-1">{error}</span>
				</Badge>
			) : (
				helper && (
					<div className="ml-3 pl-3 pr-2 text-neutral-6 text-paragraph-sm">
						{helper}
					</div>
				)
			)}
		</ClayForm.Group>
	);
};

export default Input;
