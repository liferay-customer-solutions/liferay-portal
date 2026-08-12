/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

type UsageMetricCardProps = {
	children: ReactNode;
};

export default function UsageMetricCard({children}: UsageMetricCardProps) {
	return <div className="usage-metric-card">{children}</div>;
}
