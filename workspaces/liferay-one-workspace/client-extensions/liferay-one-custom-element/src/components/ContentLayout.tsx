/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Suspense} from 'react';
import {Outlet} from 'react-router-dom';

import Breadcrumb from './Breadcrumb';

export default function ContentLayout() {
	return (
		<div className="overflow-auto pb-3 pt-3">
			<Breadcrumb />

			<Suspense fallback={null}>
				<Outlet />
			</Suspense>
		</div>
	);
}
