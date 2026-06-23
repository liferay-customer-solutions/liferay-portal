/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {IOption} from '~/pages/BusinessEvents/components/Select/Select';

function containsOption(options: IOption[], key?: string): boolean {
	if (!options.length || !key) {
		return false;
	}

	return options.some((option) => option.value === key);
}

export {containsOption};

export default containsOption;
