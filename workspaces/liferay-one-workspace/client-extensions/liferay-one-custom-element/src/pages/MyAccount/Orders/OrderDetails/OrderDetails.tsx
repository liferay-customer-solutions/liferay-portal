/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {format} from 'date-fns';
import {useParams} from 'react-router-dom';
import BackLink from '~/components/BackLink/BackLink';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {PageRenderer} from '~/components/Page/Page';
import Table from '~/components/Table/Table';
import {usePlacedOrder} from '~/hooks/usePlacedOrder';
import i18n, {translate} from '~/i18n';
import DetailsCard, {
	DetailsRow,
} from '~/pages/MyAccount/Projects/components/DetailsCard/DetailsCard';
import {
	PaymentStatus,
	getOrderStatusLabel,
	orderWorkflowDisplayType,
	paymentStatusLabels,
} from '~/utils/orderUtils';

import {STATUS_DOT_COLORS, getOrderTotal, getProjectName} from '../Orders';

import '../Orders.css';

import type {PlacedOrder} from '~/types/orders';

export default function OrderDetails() {
	const {orderId} = useParams();

	const {data: order, error, isLoading} = usePlacedOrder(orderId!);

	return (
		<div className="w-100">
			<BackLink path="..">{i18n.translate('orders-list')}</BackLink>

			<PageRenderer error={error} isLoading={isLoading}>
				{order && <OrderDetailsContent order={order} />}
			</PageRenderer>
		</div>
	);
}

function OrderDetailsContent({order}: {order: PlacedOrder}) {
	const statusDisplayType =
		orderWorkflowDisplayType[
			order.orderStatusInfo?.code as keyof typeof orderWorkflowDisplayType
		];

	const paymentStatusLabel =
		paymentStatusLabels[order.paymentStatus as PaymentStatus];

	const projectName = getProjectName(order);

	const detailsRows: DetailsRow[] = [
		{label: i18n.translate('order-id'), value: order.id},
		{
			label: i18n.translate('order-date'),
			value: order.createDate
				? format(new Date(order.createDate), 'MMM d, yyyy')
				: '-',
		},
		{label: i18n.translate('project'), value: projectName || '-'},
		{
			label: i18n.translate('status'),
			value: (
				<span className="align-items-center d-flex">
					<span
						className="orders-status-dot"
						style={{
							backgroundColor:
								STATUS_DOT_COLORS[statusDisplayType] ??
								STATUS_DOT_COLORS.secondary,
						}}
					/>

					{getOrderStatusLabel(order)}
				</span>
			),
		},
		{
			label: i18n.translate('payment-status'),
			value: paymentStatusLabel ? translate(paymentStatusLabel) : '-',
		},
		{label: i18n.translate('total'), value: getOrderTotal(order)},
		{
			label: i18n.translate('purchase-number'),
			value: order.purchaseOrderNumber || '-',
		},
		{label: i18n.translate('purchased-by'), value: order.author || '-'},
		{
			label: i18n.translate('customer-account'),
			value: order.account || '-',
		},
	];

	const orderItems = order.placedOrderItems ?? [];

	return (
		<>
			<DetailsCard rows={detailsRows} title="order-summary" />

			<DetailedCard
				cardIconAltText={i18n.translate('order-items')}
				cardTitle={i18n.translate('order-items')}
				className="mt-3"
				clayIcon="order-form"
			>
				{orderItems.length ? (
					<Table
						className="mt-3 table-borderless"
						columns={[
							{
								key: 'name',
								render: (value, item) => {
									const name = value as string | undefined;

									return (
										<span className="align-items-center d-flex">
											{(item.thumbnail as
												| string
												| undefined) && (
												<img
													alt={name}
													className="mr-2 rounded"
													draggable={false}
													src={
														item.thumbnail as string
													}
													style={{
														height: '2rem',
														width: '2rem',
													}}
												/>
											)}

											<span style={{fontWeight: 600}}>
												{name}
											</span>
										</span>
									);
								},
								title: i18n.translate('product'),
							},
							{
								key: 'sku',
								render: (value) => (value as string) || '-',
								title: i18n.translate('sku'),
							},
							{
								key: 'version',
								render: (value) => (value as string) || '-',
								title: i18n.translate('version'),
							},
							{
								key: 'quantity',
								render: (value) => (value as number) ?? '-',
								title: i18n.translate('quantity'),
							},
							{
								key: 'price',
								render: (value) => {
									const price = value as
										| {priceFormatted?: string}
										| undefined;

									return price?.priceFormatted ?? '-';
								},
								title: i18n.translate('price'),
							},
						]}
						hasHover={false}
						rows={orderItems}
					/>
				) : (
					<p className="mt-3 text-neutral-7">
						{i18n.translate('no-results-found')}
					</p>
				)}
			</DetailedCard>
		</>
	);
}
