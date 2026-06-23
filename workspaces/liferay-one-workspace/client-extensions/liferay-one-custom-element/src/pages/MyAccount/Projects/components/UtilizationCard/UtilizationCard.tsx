/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n from '~/i18n';

export default function UtilizationCard() {
	return (
		<DetailedCard
			cardIconAltText={i18n.translate('utilization')}
			cardTitle={i18n.translate('usage')}
			className="mt-3"
			clayIcon="analytics"
		>
			<p className="mt-3 text-neutral-7">
				{i18n.translate('no-usage-data-yet')}
			</p>
		</DetailedCard>
	);
}
