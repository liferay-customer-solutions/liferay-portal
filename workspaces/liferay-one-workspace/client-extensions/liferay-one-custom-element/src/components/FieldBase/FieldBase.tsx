/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayForm from '@clayui/form';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {ReactNode} from 'react';
import {Tooltip} from '~/components/Tooltip/Tooltip';

import './FieldBase.css';

export function RequiredMask() {
	return (
		<>
			<span className="field-base-required-asterisk">
				<ClayIcon
					className="field-base-required-asterisk-icon ml-1 text-danger"
					symbol="asterisk"
				/>
			</span>

			<span className="hide-accessible sr-only">Mandatory</span>
		</>
	);
}

interface FieldBaseProps {
	children?: ReactNode;
	className?: string;
	description?: string;
	disabled?: boolean;
	errorMessage?: string;
	helpMessage?: string;
	hideFeedback?: boolean;
	id?: string;
	label?: ReactNode;
	labelClassName?: string;
	localized?: boolean;
	localizedTooltipText?: string;
	required?: boolean;
	tooltip?: string;
	tooltipText?: string;
	warningMessage?: string;
}

export function FieldBase({
	children,
	className,
	description,
	disabled,
	errorMessage,
	helpMessage,
	hideFeedback,
	id,
	label,
	labelClassName,
	localized,
	localizedTooltipText,
	required,
	tooltip,
	tooltipText,
	warningMessage,
}: FieldBaseProps) {
	return (
		<ClayForm.Group
			className={classNames(className, {
				'has-error': errorMessage,
				'has-warning': warningMessage && !errorMessage,
			})}
		>
			<div className="d-flex field-base-container justify-content-between">
				<div
					className={classNames(
						'align-items-center d-flex field-base-container_label justify-content-between',
						{
							'w-100': !localized,
						}
					)}
				>
					{typeof label === 'string' ? (
						<label
							className={classNames(labelClassName, {disabled})}
							htmlFor={id}
						>
							{label}

							{required && <RequiredMask />}
						</label>
					) : (
						label
					)}

					{tooltip && (
						<div className="field-base-tooltip mb-2 ml-3">
							<Tooltip
								tooltip={tooltip}
								tooltipText={tooltipText}
							/>
						</div>
					)}
				</div>

				{localized && (
					<div className="align-items-center d-flex field-base-localized-field justify-content-start">
						<ClayButton displayType={null}>
							English (US)
							<ClayIcon
								className="arrow-down-icon"
								symbol="caret-bottom"
							/>
						</ClayButton>

						<>
							&nbsp;
							<Tooltip
								tooltip={
									localizedTooltipText ?? 'choose a language'
								}
								tooltipText={tooltipText}
							/>
						</>
					</div>
				)}
			</div>

			{description && (
				<span className="field-base-description font-weight-normal">
					{description}
				</span>
			)}

			{children}

			{errorMessage && (
				<div className="field-base-feedback text-danger">
					{errorMessage}
				</div>
			)}

			{!hideFeedback && helpMessage && (
				<div className="field-base-feedback">{helpMessage}</div>
			)}
		</ClayForm.Group>
	);
}
