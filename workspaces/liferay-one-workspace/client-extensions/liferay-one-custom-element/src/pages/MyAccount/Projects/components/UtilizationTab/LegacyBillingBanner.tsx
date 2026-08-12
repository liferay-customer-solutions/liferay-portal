/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import i18n from '~/i18n';
import {getIconSpriteMap} from '~/services/liferay/liferay';

export default function LegacyBillingBanner() {
	return (
		<ClayAlert
			className="mb-0 mt-3"
			displayType="info"
			spritemap={getIconSpriteMap()}
			title={i18n.translate('this-project-is-on-a-legacy-billing-model')}
		>
			{i18n.translate(
				'project-metrics-are-available-for-liferay-saas-customers-on-liferays-latest-usage-based-model'
			)}
		</ClayAlert>
	);
}
