/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {ReactNode} from 'react';

type SectionProps = {
	children: ReactNode;
	className?: string;
	label: string;
	required?: boolean;
};

const Section = ({children, className, label, required}: SectionProps) => (
	<div className={classNames('mb-4', className)}>
		<label className="d-block font-weight-semi-bold mb-2">
			{label}

			{required && <span className="ml-1 text-danger">*</span>}
		</label>

		{children}
	</div>
);

export default Section;
