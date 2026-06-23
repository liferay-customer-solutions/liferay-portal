/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useModal} from '@clayui/core';
import {useMemo} from 'react';
import {Link} from 'react-router-dom';
import ListView, {ListViewProps} from '~/components/ListView/ListView';
import {ManagementToolbarProps} from '~/components/ManagementToolbar/ManagementToolbar';
import {useOneContext} from '~/context/OneContextProvider';
import i18n from '~/i18n';
import CreateTrialModalForm from '~/pages/Admin/SSADashboard/components/CreateTrialModalForm';
import ExtensionStatus from '~/pages/Admin/SSADashboard/components/ExtensionStatus/ExtensionStatus';
import TrialStatus from '~/pages/Admin/SSADashboard/components/TrialStatus/TrialStatus';
import {useSSADashboardOutlet} from '~/pages/Admin/SSADashboard/hooks/useSSADashboardOutlet';
import {EXTEND_TRIAL_STATUS_LABEL} from '~/pages/Admin/SSADashboard/utils/constants';
import {Liferay} from '~/services/liferay/liferay';
import SearchBuilder from '~/utils/SearchBuilder';
import {Action} from '~/utils/appConstants';
import {formatDate, formatDateTime} from '~/utils/dateUtils';
import {OrderCustomFields} from '~/utils/orderUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import type {APIResponse} from '~/types/api';
import type {PlacedOrder} from '~/types/orders';
import type {TrialExtend} from '~/types/trial';

type TrialsListViewProps = {
	actions: Action[];
	authorOnlyTrials?: boolean;
	createTrialFormModal: ReturnType<typeof useModal>;
	isSortable?: boolean;
	listViewProps?: Partial<ListViewProps<PlacedOrder>>;
	managementToolbarProps?: {
		visible?: boolean;
	} & Omit<
		ManagementToolbarProps,
		| 'actions'
		| 'onSelectAllRows'
		| 'rowSelectable'
		| 'tableProps'
		| 'totalItems'
	>;
	parentPath?: string;
};

const refreshInterval = 60 * 1000;

const getResourceURL = (accountId: number) =>
	`/o/headless-commerce-delivery-order/v1.0/channels/${Liferay.CommerceContext.commerceChannelId}/accounts/${accountId}/placed-orders?${new URLSearchParams(
		{
			nestedFields: 'placedOrderItems',
			sort: 'createDate:desc',
		}
	)}`;

export default function TrialListView({
	actions,
	authorOnlyTrials,
	createTrialFormModal,
	listViewProps,
	managementToolbarProps,
	parentPath,
}: TrialsListViewProps) {
	const {ssaAccount, ssaTrialExtend} = useSSADashboardOutlet();
	const {myUserAccount} = useOneContext();

	const {properties} = useOneContext();

	const isFilterByAuthorIdEnabled =
		properties.featureFlags.includes('LPD-63837');

	const authorFilter = isFilterByAuthorIdEnabled ? 'authorId' : 'author';

	const authorFilterValue = isFilterByAuthorIdEnabled
		? myUserAccount?.id
		: myUserAccount?.name;

	const searchBuilder = useMemo(() => {
		const searchBuilder = new SearchBuilder().eq(
			'orderTypeExternalReferenceCode',
			'SSA_SAAS'
		);

		if (authorOnlyTrials) {
			searchBuilder.and().eq(authorFilter, authorFilterValue, {
				unquote: isFilterByAuthorIdEnabled,
			});
		}

		return searchBuilder;
	}, [
		authorFilter,
		authorFilterValue,
		authorOnlyTrials,
		isFilterByAuthorIdEnabled,
	]);

	if (!ssaAccount) {
		return null;
	}

	return (
		<>
			<ListView<PlacedOrder>
				defaultFilters={{filter: searchBuilder.build()}}
				emptyStateProps={{title: i18n.translate('no-trials-yet')}}
				id="ssa-trials"
				managementToolbarProps={{
					filterSchema: 'administratorSSATrials',
					...managementToolbarProps,
				}}
				refreshInterval={refreshInterval}
				resource={getResourceURL(ssaAccount.id)}
				tableProps={{
					actions,
					columns: [
						{
							id: 'placedOrderItems',
							name: i18n.translate('project-id'),
							render: (_, {customFields, id}) => (
								<Link
									className="font-weight-semi-bold ml-2"
									to={
										parentPath
											? `/details/${id}?from=${parentPath}`
											: `/details/${id}`
									}
								>
									{customFields &&
										safeJSONParse(
											customFields[
												OrderCustomFields.TRIAL_SETTINGS
											],
											{projectId: id}
										).projectId}
								</Link>
							),
						},
						{
							id: 'author',
							name: i18n.translate('created-by'),
							render: (author, {createDate}) => (
								<div className="d-flex flex-column">
									<span className="dashboard-table-row-text">
										{author}
									</span>

									<span className="dashboard-table-row-purchased-date">
										{formatDate(createDate)}
									</span>
								</div>
							),
							sortable: true,
						},
						{
							id: 'customFields',
							name: i18n.translate('solution-type'),
							render: (customFields) =>
								safeJSONParse(
									customFields[
										OrderCustomFields.TRIAL_SETTINGS
									],
									{siteInitializerKey: 'Blank Site'}
								).siteInitializerKey,
						},
						{
							id: 'createDate',
							name: i18n.translate('end-date'),
							render: (_, {customFields}) =>
								formatDateTime(
									customFields[
										OrderCustomFields.TRIAL_END_DATE
									],
									'DNE'
								),
							sortable: true,
						},
						{
							id: 'orderStatusInfo',
							name: i18n.translate('trial-status'),
							render: (orderStatusInfo) => (
								<TrialStatus
									trialStatus={orderStatusInfo?.label}
								/>
							),
						},
						{
							id: 'id',
							name: i18n.translate('extension-status'),
							render: (orderId, placedOrder) => {
								const ssaTrialsExtendRequests =
									ssaTrialExtend.items;

								const extendRequests =
									ssaTrialsExtendRequests?.filter(
										(extend: TrialExtend) => {
											return (
												extend.r_orderToTrialExtensionRequest_commerceOrderId ===
												Number(orderId)
											);
										}
									) as TrialExtend[];

								if (
									!extendRequests ||
									extendRequests?.length === 0
								) {
									return (
										<ExtensionStatus extensionStatus="not-requested" />
									);
								}

								return (
									<ExtensionStatus
										extensionStatus={
											placedOrder.orderStatusInfo
												.label === 'completed'
												? 'extension-expired'
												: (extendRequests[0]?.dueStatus
														.key as keyof typeof EXTEND_TRIAL_STATUS_LABEL)
										}
									/>
								);
							},
						},
					],
				}}
				{...listViewProps}
			>
				{(_, {mutate}) => (
					<CreateTrialModalForm
						modal={createTrialFormModal}
						mutate={
							mutate as unknown as (
								fn: (data: APIResponse<PlacedOrder>) => unknown,
								options?: {revalidate: boolean}
							) => Promise<unknown>
						}
					/>
				)}
			</ListView>
		</>
	);
}
