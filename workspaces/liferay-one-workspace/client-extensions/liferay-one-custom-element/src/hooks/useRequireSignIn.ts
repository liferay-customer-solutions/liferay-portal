/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect} from 'react';
import {Liferay} from '~/services/liferay/liferay';

const useRequireSignIn = () => {
	const isSignedIn = Liferay.ThemeDisplay.isSignedIn();

	useEffect(() => {
		if (!isSignedIn) {
			Liferay.Util.navigate(
				`/c/portal/login?redirect=${encodeURIComponent(
					window.location.pathname +
						window.location.search +
						window.location.hash
				)}`
			);
		}
	}, [isSignedIn]);

	return isSignedIn;
};

export default useRequireSignIn;
