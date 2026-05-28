/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLabel from '@clayui/label';
import {Status} from '@clayui/modal/lib/types';
import {formatDistance} from 'date-fns';

import {DashboardPage} from '../../../../components/DashBoardPage/DashboardPage';
import {DashboardEmptyTable} from '../../../../components/DashboardTable/DashboardEmptyTable';
import Loading from '../../../../components/Loading';
import Table from '../../../../components/Table/Table';
import {OrderWorkflowStatusCode} from '../../../../enums/Order';
import i18n from '../../../../i18n';

const ORDER_STATUS_LABEL = {
	Approved: 'success',
	Completed: 'success',
	Open: 'secondary',
	Pending: 'warning',
};

type TrialTableProps = {
	items: Order[];
};

const TrialTable: React.FC<TrialTableProps> = ({items}) => (
	<DashboardPage
		messages={{description: '', title: i18n.translate('trials')}}
	>
		{items.length ? (
			<Table
				className="mt-3"
				columns={[
					{
						key: 'id',
						render: (id) => (
							<span className="font-weight-bold">{id}</span>
						),
						title: i18n.translate('id'),
					},
					{
						key: 'orderItems',
						render: (orderItems) => orderItems[0]?.name.en_US,
						title: i18n.translate('product'),
					},
					{
						key: 'account',
						render: (account) => account?.name,
						title: i18n.translate('user-account'),
					},
					{
						key: 'orderStatusInfo',
						render: (orderStatusInfo) => (
							<div className="align-items-center d-flex">
								<ClayLabel
									className="text-nowrap"
									displayType={
										ORDER_STATUS_LABEL[
											orderStatusInfo?.label as keyof typeof ORDER_STATUS_LABEL
										] as Status
									}
								>
									{orderStatusInfo?.label_i18n}
								</ClayLabel>

								{[
									OrderWorkflowStatusCode.ON_HOLD,
									OrderWorkflowStatusCode.PROCESSING,
								].includes(orderStatusInfo.code) && (
									<Loading
										displayType="primary"
										shape="circle"
										size="sm"
									/>
								)}
							</div>
						),
						title: i18n.translate('trial-status'),
					},
					{
						key: 'createDate',
						render: (createDate) => (
							<span className="ml-2 text-capitalize text-nowrap">
								{createDate &&
									formatDistance(
										new Date(createDate),
										Date.now(),
										{addSuffix: true}
									)}
							</span>
						),
						title: i18n.translate('created-at'),
					},
					{
						key: 'customFields',
						render: (customFields) => (
							<span className="ml-2 text-capitalize text-nowrap">
								{customFields['trial-start-date'] &&
									formatDistance(
										new Date(
											customFields['trial-start-date']
										),
										Date.now(),
										{addSuffix: true}
									)}
							</span>
						),
						title: i18n.translate('start-date'),
					},
					{
						key: 'customFields',
						render: (customFields) => (
							<span className="ml-2 text-capitalize text-nowrap">
								{customFields['trial-end-date'] &&
									formatDistance(
										new Date(
											customFields['trial-end-date']
										),
										Date.now(),
										{addSuffix: true}
									)}
							</span>
						),
						title: i18n.translate('expiration-date'),
					},
				]}
				rows={items}
			/>
		) : (
			<div className="mt-3">
				<DashboardEmptyTable
					description1={i18n.translate(
						'purchase-and-install-new-apps-and-they-will-show-up-here'
					)}
					description2={i18n.translate(
						'click-on-browse-catalog-to-start'
					)}
					icon="grid"
					title={i18n.translate('no-orders-yet')}
				/>
			</div>
		)}
	</DashboardPage>
);

export default TrialTable;
