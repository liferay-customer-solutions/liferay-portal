/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {ReactNode} from 'react';
import {Tooltip} from '~/components/Tooltip/Tooltip';

type SectionProps = {
	children: ReactNode;
	className?: string;
	description?: string;
	disabled?: boolean;
	label: string;
	required?: boolean;
	tooltip?: string;
	tooltipText?: string;
};

const Section = ({
	children,
	className,
	description,
	disabled,
	label,
	required,
	tooltip,
	tooltipText,
}: SectionProps) => (
	<div className={classNames('mb-4', className, {'text-muted': disabled})}>
		<label className="align-items-center d-flex font-weight-semi-bold mb-2">
			{label}

			{required && <span className="ml-1 text-danger">*</span>}

			{(tooltip || tooltipText) && (
				<Tooltip tooltip={tooltip} tooltipText={tooltipText} />
			)}
		</label>

		{description && <p className="mb-2 text-muted">{description}</p>}

		{children}
	</div>
);

export {Section};
export default Section;
