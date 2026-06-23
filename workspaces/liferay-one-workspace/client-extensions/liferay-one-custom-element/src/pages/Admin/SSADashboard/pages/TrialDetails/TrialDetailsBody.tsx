/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import DOMPurify from 'dompurify';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import QATable, {Orientation} from '~/components/QATable/QATable';
import {useOneContext} from '~/context/OneContextProvider';
import i18n from '~/i18n';
import DeliveryOrderModel from '~/models/DeliveryOrderModel';
import {DeliveryProductModel} from '~/models/DeliveryProductModel';
import ExtensionStatus from '~/pages/Admin/SSADashboard/components/ExtensionStatus/ExtensionStatus';
import TrialStatus from '~/pages/Admin/SSADashboard/components/TrialStatus/TrialStatus';
import {useSSADashboardOutlet} from '~/pages/Admin/SSADashboard/hooks/useSSADashboardOutlet';
import {EXTEND_TRIAL_STATUS_LABEL} from '~/pages/Admin/SSADashboard/utils/constants';
import {formatDate, formatDateTime} from '~/utils/dateUtils';
import {OrderWorkflowStatusCode} from '~/utils/orderUtils';

import ExternalLink from './ExternalLink/ExternalLink';

import type {PlacedOrder} from '~/types/orders';

type TrialDetailsBodyProps = {
	orderModel: DeliveryOrderModel;
	placedOrder: PlacedOrder;
	productModel: DeliveryProductModel;
	projectId: string;
};

const TrialDetailsBody: React.FC<TrialDetailsBodyProps> = ({
	orderModel,
	placedOrder,
	productModel,
	projectId,
}) => {
	const {properties} = useOneContext();

	const ssaProject = `${properties.ssaProjectPrefix}-ext${projectId}`;

	const {ssaTrialExtend} = useSSADashboardOutlet();

	const extensionStatus =
		placedOrder?.orderStatusInfo?.code === OrderWorkflowStatusCode.COMPLETED
			? 'extension-expired'
			: ssaTrialExtend?.items?.find(
					(trialExtend) => trialExtend.projectId === projectId
				)?.dueStatus?.key;

	return (
		<div className="d-flex justify-content-between mt-2 row">
			<DetailedCard
				cardIconAltText="Profile Icon"
				cardTitle={i18n.translate('details')}
				className="col-6"
				clayIcon="order-form-tag"
			>
				<span>
					<span className="h4 mt-4 text-black-50">
						{i18n.translate('general-info')}
					</span>

					<hr className="my-0" />

					<QATable
						items={[
							{
								title: i18n.translate('account-name'),

								value: (
									<div className="mb-3">
										{placedOrder?.account}
									</div>
								),
							},
							{
								title: i18n.translate('created-by'),
								value: (
									<div className="mb-3">
										{placedOrder?.author}
									</div>
								),
							},
							{
								title: i18n.translate('type'),
								value: (
									<div className="mb-3">
										{placedOrder?.orderType}
									</div>
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</span>

				<span>
					<span className="h4 mt-4 text-black-50">
						{i18n.translate('order-info')}
					</span>
					<hr className="my-0" />

					<QATable
						items={[
							{
								title: i18n.translate('order-id'),
								value: (
									<div className="mb-3">
										{placedOrder?.id}
									</div>
								),
							},
							{
								title: i18n.translate('order-date'),
								value: (
									<div>
										{placedOrder?.createDate &&
											formatDate(
												placedOrder?.createDate as string
											)}
									</div>
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</span>
			</DetailedCard>

			<DetailedCard
				cardIconAltText="Profile Icon"
				cardTitle={i18n.translate('ssa-trial-summary')}
				className="col-6"
				clayIcon="date-time"
			>
				<span>
					<span className="h4 mt-4 text-black-50">
						{i18n.translate('trial-info')}
					</span>

					<hr className="my-0" />

					<QATable
						items={[
							{
								title: i18n.translate('trial-start-date'),
								value: formatDateTime(
									orderModel.customFields.TRIAL_START_DATE
								),
								visible: !orderModel.isCancelled,
							},
							{
								title: i18n.translate('trial-end-date'),
								value: formatDateTime(
									orderModel.customFields.TRIAL_END_DATE
								),
								visible: !orderModel.isCancelled,
							},
							{
								className: 'mb-2',
								title: i18n.translate('trial-url'),
								value: (
									<ExternalLink
										className="h5 py-1"
										href={`https://${orderModel.customFields.TRIAL_VIRTUAL_HOST}`}
									>
										{
											orderModel.customFields
												.TRIAL_VIRTUAL_HOST
										}
									</ExternalLink>
								),
								visible:
									OrderWorkflowStatusCode.IN_PROGRESS ===
									placedOrder?.orderStatusInfo?.code,
							},
							{
								title: i18n.translate('cloud-project'),
								value: (
									<ExternalLink
										className="h5"
										href={`${properties.cloudConsoleURL}/projects/${ssaProject}`}
									>
										{ssaProject}
									</ExternalLink>
								),
								visible:
									OrderWorkflowStatusCode.IN_PROGRESS ===
									placedOrder?.orderStatusInfo?.code,
							},
							{
								title: i18n.translate('trial-status'),
								value: (
									<div className="mb-3">
										<TrialStatus
											trialStatus={
												placedOrder?.orderStatusInfo
													?.label as string
											}
										/>
									</div>
								),
							},
							{
								title: i18n.translate('trial-error'),
								value: (
									<div className="mb-3">
										Something went wrong during the Trial
										Provisioning
									</div>
								),
								visible: orderModel.isCancelled,
							},
							{
								title: i18n.translate('extension-status'),
								value: (
									<ExtensionStatus
										className="my-3"
										extensionStatus={
											extensionStatus as keyof typeof EXTEND_TRIAL_STATUS_LABEL
										}
									/>
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</span>

				<span>
					<span className="h4 mt-4 text-black-50">
						{i18n.translate('description')}
					</span>
					<hr className="my-0" />

					<p
						className="app-review-section-body-description-paragraph mt-3"
						dangerouslySetInnerHTML={{
							__html: DOMPurify.sanitize(
								productModel.description
							),
						}}
					/>
				</span>
			</DetailedCard>
		</div>
	);
};

export default TrialDetailsBody;
