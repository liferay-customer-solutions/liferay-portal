/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import type {Word} from '~/i18n';

export function getTaxonomyCategoryLabel(name: string) {
	return i18n.translate(name as Word);
}

export default getTaxonomyCategoryLabel;
