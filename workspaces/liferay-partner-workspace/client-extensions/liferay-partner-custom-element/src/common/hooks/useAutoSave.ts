/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

export default function useAutoSave(
	autoSaveFunction: Function,
	autoSaveTime: number,
	monitoredObject: object,
	shouldMonitor: boolean
) {
	const [url] = useState<string>(window.location.href);

	useEffect(() => {
		if (shouldMonitor) {
			const interval = setTimeout(() => {
				if (url === window.location.href) {
					autoSaveFunction();
				}
			}, autoSaveTime);

			return () => clearTimeout(interval);
		}
		// eslint-disable-next-line react-hooks/exhaustive-deps
	}, [monitoredObject]);
}
