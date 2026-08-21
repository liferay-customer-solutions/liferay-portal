/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {format} from 'date-fns';
import {useOutletContext} from 'react-router-dom';

import {DetailedCard} from '../../../../../components/DetailedCard/DetailedCard';
import {PageRenderer} from '../../../../../components/Page';
import QATable from '../../../../../components/QATable';
import {
	OrderCustomFields,
	OrderWorkflowStatusCode,
} from '../../../../../enums/Order';
import {SolutionTypes} from '../../../../../enums/Product';
import i18n from '../../../../../i18n';
import LiferayProductsAlerts from '../LiferayProductsAlerts';
import WorkspaceInfoCard from './WorkspaceInfoCard';

const AnalyticsDetails = () => {
	const {marketplaceDeliveryProduct, placedOrder} = useOutletContext<any>();

	const orderStatusCode = placedOrder?.orderStatusInfo
		?.code as OrderWorkflowStatusCode;

	const orderMetadata = placedOrder
		? JSON.parse(placedOrder.customFields[OrderCustomFields.ORDER_METADATA])
		: {};

	const {analyticsProject} = orderMetadata;

	// This component is the fallback for every ADDONS order, which also covers
	// the legacy Analytics Cloud add-on. Liferay Data Platform splits the
	// workspace information into its own Environment tab, so only the legacy
	// add-on keeps it alongside the details.

	const isLiferayDataPlatform =
		marketplaceDeliveryProduct?.specificationValues?.SOLUTION_TYPE ===
		SolutionTypes.LIFERAY_DATA_PLATFORM;

	return (
		<PageRenderer>
			<LiferayProductsAlerts orderStatusCode={orderStatusCode} />

			<div
				className={classNames('app-details-body-container', {
					'mt-4': isLiferayDataPlatform,
				})}
			>
				<DetailedCard
					cardIconAltText="Details Icon"
					cardTitle={i18n.translate('details')}
					clayIcon="order-form-tag"
				>
					<QATable
						items={[
							{
								title: i18n.translate('order-id'),
								value: placedOrder?.id,
							},
							{
								title: i18n.translate('order-date'),
								value: format(
									new Date(placedOrder?.createDate || ''),
									'dd MMM, yyyy'
								),
							},
							{
								title: i18n.translate('account-name'),
								value: placedOrder?.account,
							},
							{
								title: i18n.translate('customer-project'),
								value: analyticsProject?.corpProjectName,
							},
							{
								title: i18n.translate('purchased-by'),
								value: placedOrder?.author,
							},
							{
								title: i18n.translate('purchase-number'),
								value: placedOrder.id,
							},
							{
								title: i18n.translate('subscription-type'),
								value: placedOrder?.placedOrderItems[0].sku,
							},
						]}
					/>
				</DetailedCard>

				{!isLiferayDataPlatform && (
					<WorkspaceInfoCard analyticsProject={analyticsProject} />
				)}
			</div>
		</PageRenderer>
	);
};

export default AnalyticsDetails;
