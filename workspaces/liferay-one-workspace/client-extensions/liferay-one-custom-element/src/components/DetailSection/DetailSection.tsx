/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

type DetailSectionProps = {
	children: ReactNode;
	isLastSection?: boolean;
	title: string;
};

export default function DetailSection({
	children,
	isLastSection = false,
	title,
}: DetailSectionProps) {
	return (
		<>
			<div className="mb-4">
				<h3 className="mb-3">{title}</h3>

				{children}
			</div>

			{!isLastSection && <hr />}
		</>
	);
}
