/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import i18n from '~/i18n';
import {getIconSpriteMap} from '~/services/liferay/liferay';

export default function UsageUnavailableBanner() {
	return (
		<ClayAlert
			className="mb-3"
			displayType="info"
			spritemap={getIconSpriteMap()}
			title={i18n.translate('usage-data-not-loaded')}
		>
			{i18n.translate(
				'current-usage-could-not-be-loaded-the-limits-shown-come-from-your-entitlements'
			)}
		</ClayAlert>
	);
}
