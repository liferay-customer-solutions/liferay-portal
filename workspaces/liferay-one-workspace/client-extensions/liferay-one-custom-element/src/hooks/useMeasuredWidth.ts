/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useLayoutEffect, useRef, useState} from 'react';

export function useMeasuredWidth<T extends HTMLElement>(
	shouldMeasure: boolean
) {
	const ref = useRef<T>(null);
	const [width, setWidth] = useState<number>();

	useLayoutEffect(() => {
		if (shouldMeasure && ref.current) {
			setWidth(ref.current.offsetWidth);
		}
	}, [shouldMeasure]);

	return {ref, width};
}

export default useMeasuredWidth;
