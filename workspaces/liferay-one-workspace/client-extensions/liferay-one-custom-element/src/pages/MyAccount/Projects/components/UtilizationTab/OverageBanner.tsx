/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import i18n from '~/i18n';
import {getIconSpriteMap} from '~/services/liferay/liferay';

export default function OverageBanner() {
	return (
		<ClayAlert
			className="mb-3"
			displayType="warning"
			spritemap={getIconSpriteMap()}
			title={i18n.translate('peak-usage-exceeded-your-entitlements')}
		>
			{i18n.translate(
				'your-peak-usage-for-this-billing-period-exceeded-your-entitlement-limits-overage-charges-may-apply'
			)}
		</ClayAlert>
	);
}
