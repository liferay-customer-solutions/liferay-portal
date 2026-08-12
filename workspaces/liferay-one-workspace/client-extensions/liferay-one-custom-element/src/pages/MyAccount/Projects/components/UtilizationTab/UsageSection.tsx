/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n, {Word} from '~/i18n';

export default function UsageSection({
	children,
	title,
}: {
	children: React.ReactNode;
	title: Word;
}) {
	return (
		<div className="mt-4">
			<h4 className="mb-3">{i18n.translate(title)}</h4>

			<div className="usage-dashboard-grid">{children}</div>
		</div>
	);
}
