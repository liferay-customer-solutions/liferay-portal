/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo} from 'react';

import Page from '../../../components/Page';
import i18n from '../../../i18n';
import InfoCard from '../MPSummary/components/InfoCard';
import useOrderMetrics from '../MPSummary/hooks/useOrderMetrics';
import {AdministratorOrdersListView} from './components/AdministratorOrdersListView';

export default function Orders() {
	const {data: metrics} = useOrderMetrics('week');

	const infoCards = useMemo(
		() => [
			{
				growth: metrics?.growth ?? 0,
				growthContext: `+${metrics?.lastPeriod ?? 0} this week `,
				title: 'Total Orders',
				value: metrics?.totalCount,
			},
			{
				title: 'Monthly Orders',
				value: metrics?.ordersThisMonth,
			},
			{
				title: 'Current Year Orders',
				value: metrics?.ordersThisYear,
			},
		],
		[
			metrics?.growth,
			metrics?.lastPeriod,
			metrics?.ordersThisMonth,
			metrics?.ordersThisYear,
			metrics?.totalCount,
		]
	);

	return (
		<>
			<div className="d-flex flex-column">
				<div
					className="d-flex flex-wrap info-container"
					style={{marginBottom: '1rem'}}
				>
					{infoCards.map((infoCard, index) => (
						<InfoCard
							{...infoCard}
							key={index}
							symbol="shopping-cart"
							title={infoCard.title}
							value={infoCard.value}
						/>
					))}
				</div>
			</div>

			<Page
				pageRendererProps={{className: 'border py-2'}}
				title={i18n.translate('marketplace-orders')}
			>
				<AdministratorOrdersListView
					isSortable
					managementToolbarProps={{
						searchVisible: true,
						visible: true,
					}}
				/>
			</Page>
		</>
	);
}
