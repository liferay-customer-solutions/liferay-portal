/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';

import ErrorBoundary from '../../../components/ErrorBoundary';
import Page from '../../../components/Page';
import {useOneContext} from '../../../context/OneContext';
import i18n from '../../../i18n';
import {formatCurrency} from '../../../utils/currencies';
import AdministratorAppsListView from './components/AdministratorAppsListView';
import DonutKPIChart from './components/DonutKPIChart';
import InfoCard from './components/InfoCard';
import {AdministratorOrdersListView} from './components/AdministratorOrdersListView';
import AdministratorMostPurchasedSection from './components/AdministratorMostPurchasedSection';
import useAccountsMetrics from './hooks/useAccountsMetrics';
import useKPI from './hooks/useKPI';
import useOrderMetrics from './hooks/useOrderMetrics';

import './MPSummary.scss';

export default function MPSummary() {
	const {data: {kpis = []} = {}} = useKPI();
	const {data: accounts} = useAccountsMetrics('week');
	const {data: orderMetrics} = useOrderMetrics('week');
	const {marketplaceUserAccount} = useOneContext();

	const infoCards = useMemo(
		() => [
			{
				growth: accounts?.growth ?? 0,
				growthContext: `+${accounts?.lastPeriod ?? 0} this week `,
				symbol: 'users',
				title: i18n.translate('accounts'),
				value: accounts?.totalCount ?? 0,
			},
			{
				symbol: 'dollar-symbol',
				title: (
					<span>
						{i18n.translate('income')}{' '}
						&ensp;&ensp;&ensp;&ensp;&ensp;&ensp;
					</span>
				),
				value: formatCurrency(orderMetrics?.totalAmount || 0),
			},
			{
				growth: orderMetrics?.growth ?? 0,
				growthContext: `+${orderMetrics?.lastPeriod ?? 0} this week `,
				symbol: 'shopping-cart',
				title: i18n.translate('orders'),
				value: orderMetrics?.totalCount ?? 0,
			},
		],
		[
			accounts?.growth,
			accounts?.lastPeriod,
			accounts?.totalCount,
			orderMetrics?.growth,
			orderMetrics?.lastPeriod,
			orderMetrics?.totalAmount,
			orderMetrics?.totalCount,
		]
	);

	return (
		<Page
			description={i18n.translate(
				'a-sleek-and-intuitive-admin-dashboard-for-monitoring-key-metrics'
			)}
			title={i18n.translate('administrator-dashboard')}
		>
			<div className="d-flex flex-column">
				<div className="d-flex flex-wrap mb-3" style={{gap: '20px'}}>
					<ErrorBoundary>
						{kpis.map((chart, index) => (
							<DonutKPIChart {...chart} key={index} />
						))}
					</ErrorBoundary>
				</div>

				<div
					className="d-flex flex-wrap info-container"
					style={{marginBottom: '3.5rem'}}
				>
					{infoCards.map((infoCard, index) => (
						<InfoCard {...infoCard} key={index} />
					))}
				</div>

				<div style={{marginBottom: '3.5rem'}}>
					<Page
						pageRendererProps={{
							className: 'border py-2 rounded-lg',
						}}
						rightButton={
							marketplaceUserAccount?.isAdmin && (
								<Link
									className="font-weight-bold"
									to="/admin/mp-orders"
								>
									{i18n.translate('view-all')}
									<ClayIcon symbol="order-arrow-right" />
								</Link>
							)
						}
						title={i18n.translate('recent-orders')}
					>
						<AdministratorOrdersListView
							listViewProps={{
								id: 'summary-orders',
								initialContext: {pageSize: 5},
								paginationOptions: {displayType: false},
							}}
							managementToolbarProps={{
								visible: false,
							}}
						/>
					</Page>
				</div>

				<div style={{marginBottom: '3.5rem'}}>
					<Page
						pageRendererProps={{
							className: 'border py-2 rounded-lg',
						}}
						rightButton={
							<Link
								className="font-weight-bold"
								to="/admin/mp-apps"
							>
								{i18n.translate('view-all')}
								<ClayIcon symbol="order-arrow-right" />
							</Link>
						}
						title={i18n.translate('published-apps')}
					>
						<AdministratorAppsListView
							isSortable
							listViewProps={{
								id: 'summary-apps',
								initialContext: {pageSize: 5},
								paginationOptions: {displayType: false},
							}}
							managementToolbarProps={{
								visible: false,
							}}
						/>
					</Page>
				</div>

				<AdministratorMostPurchasedSection />
			</div>
		</Page>
	);
}
