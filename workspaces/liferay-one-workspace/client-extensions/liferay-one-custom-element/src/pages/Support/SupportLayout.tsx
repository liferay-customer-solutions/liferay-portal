/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Suspense} from 'react';
import {Outlet} from 'react-router-dom';

export default function SupportLayout() {
	return (
		<Suspense fallback={null}>
			<Outlet />
		</Suspense>
	);
}
