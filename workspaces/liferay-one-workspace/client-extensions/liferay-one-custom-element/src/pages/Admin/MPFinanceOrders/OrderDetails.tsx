/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useParams} from 'react-router-dom';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {PageRenderer} from '~/components/Page/Page';
import PaymentDetailsHeader from '~/components/PaymentDetailsHeader/PaymentDetailsHeader';
import PaymentStatusBadge from '~/components/PaymentStatusBadge/PaymentStatusBadge';
import QATable, {Orientation} from '~/components/QATable/QATable';
import Table from '~/components/Table/Table';
import useAdminOrderProduct from '~/hooks/useAdminOrderProduct';
import i18n from '~/i18n';
import HeadlessCommerceAdminOrder from '~/services/headless/HeadlessCommerceAdminOrder';
import {Liferay} from '~/services/liferay/liferay';
import {formatDateTime} from '~/utils/dateUtils';
import {formatAddress} from '~/utils/formatAddress';
import {formatCurrency} from '~/utils/formatCurrency';
import {
	OrderCustomFields,
	PaymentStatus as PaymentStatusCode,
} from '~/utils/orderUtils';
import {ProductSpecificationKey} from '~/utils/productUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import type {BillingAddress} from '~/types/orders';

function textWrapper(content: string | number | undefined) {
	if (content === undefined || content === null || content === '') {
		return <p className="mb-2 mt-1">-</p>;
	}

	return <p className="mb-2 mt-1">{content}</p>;
}

const OrderDetails = () => {
	const {orderId} = useParams();

	const {data, error, isLoading, mutate} = useAdminOrderProduct(orderId!!);

	const {order, payments, product} = data || {};

	const koroneikiProjects = safeJSONParse(
		order?.customFields![OrderCustomFields.KORONEIKI_PROJECT] || '[]',
		[]
	);

	const koroneikiProjectNames = koroneikiProjects.map(
		(project: {name: string}) => project?.name
	);

	const currencyCode = order?.currencyCode || 'USD';

	return (
		<PageRenderer
			className="app-details-header d-flex flex-column w-100"
			error={error}
			isLoading={isLoading}
		>
			<PaymentDetailsHeader
				backLink="/mp-finance-orders"
				onClick={async () =>
					HeadlessCommerceAdminOrder.patchOrder(orderId as string, {
						paymentStatus: PaymentStatusCode.PAID,
					})
						.then((updatedOrder) => {
							mutate(
								{
									...data!,
									order: {
										...data!.order,
										...updatedOrder,
										paymentStatus: PaymentStatusCode.PAID,
									},
								},
								{revalidate: false}
							);

							Liferay.Util.openToast({
								message: 'Order marked as paid.',
								type: 'success',
							});
						})
						.catch(() =>
							Liferay.Util.openToast({
								message: 'Oops! Something went wrong.',
								type: 'danger',
							})
						)
				}
				paymentStatusCode={order?.paymentStatusInfo.code as number}
				showButton={[
					PaymentStatusCode.FAILED,
					PaymentStatusCode.PAYMENT_PENDING,
					PaymentStatusCode.PENDING,
				].includes(order?.paymentStatusInfo.code as number)}
				title={orderId as string}
			/>

			<div className="d-flex mt-5">
				<DetailedCard
					cardIconAltText="order-form-pencil"
					cardTitle={i18n.translate('account-details')}
					className="mr-5 w-100"
					clayIcon="order-form-pencil"
				>
					<QATable
						items={[
							{
								title: i18n.translate('account-name'),
								value: textWrapper(order?.account?.name),
							},
							{
								title: i18n.translate('user-email'),
								value: textWrapper(order?.creatorEmailAddress),
							},
							{
								title: i18n.translate('project'),
								value: textWrapper(
									koroneikiProjectNames?.length
										? koroneikiProjectNames.join(', ')
										: '-'
								),
							},
							{
								title: i18n.translate('address'),
								value: textWrapper(
									formatAddress(
										order?.billingAddress as BillingAddress
									)
								),
							},
							{
								title: i18n.translate('vat-number'),
								value: textWrapper(order?.account?.taxId),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>

				<DetailedCard
					cardIconAltText="change-list"
					cardTitle={i18n.translate('transaction-details')}
					className="w-100"
					clayIcon="change-list"
				>
					<QATable
						items={[
							{
								title: i18n.translate('purchase-date'),
								value: textWrapper(
									formatDateTime(order?.createDate)
								),
							},
							{
								title: i18n.translate('payment-method'),
								value: textWrapper(
									order?.paymentMethod ===
										'paypal-integration'
										? i18n.translate('paypal')
										: i18n.translate('offline-payment')
								),
							},
							{
								title: i18n.translate('transaction-id'),
								value: textWrapper(order?.transactionId),
							},
							{
								title: i18n.translate('fulfillment-date'),
								value: textWrapper(
									formatDateTime(order?.orderDate)
								),
							},
							{
								title: i18n.translate('payment-status'),
								value: (
									<PaymentStatusBadge
										paymentStatus={
											order?.paymentStatusInfo
												.code as number
										}
									/>
								),
							},
							{
								title: i18n.translate('error-details'),
								value: (
									payments?.items[0] as {
										errorMessages?: string;
									}
								)?.errorMessages,
								visible:
									!!(
										payments?.items[0] as {
											errorMessages?: string;
										}
									)?.errorMessages &&
									order?.paymentStatus ===
										PaymentStatusCode.FAILED,
							},
							{
								className: 'mt-2',
								title: i18n.translate('paid-date'),
								value: textWrapper(
									formatDateTime(
										(
											payments?.items?.[0] as {
												createDate?: string;
											}
										)?.createDate
									)
								),
								visible:
									order?.paymentStatus ===
									PaymentStatusCode.PAID,
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>
			</div>

			<DetailedCard
				cardIconAltText="order-form"
				cardTitle={i18n.translate('order-details')}
				className="mt-5 pb-0 w-100"
				clayIcon="order-form"
			>
				<Table
					className="table-borderless"
					columns={[
						{
							key: 'options',
							render: (value, {quantity}) => {
								const [skuOption] = safeJSONParse(
									value as string,
									[
										{
											skuOptionValueKey: 'Standard',
											skuOptionValueName: 'Standard',
										},
									]
								);

								return (
									<div className="pt-2">
										<div className="d-flex">
											<img
												alt="App Icon"
												className="mr-2 order-details-app-icon rounded"
												draggable={false}
												src={product?.thumbnail}
											/>

											<span className="d-flex flex-column">
												<span className="font-weight-bold">
													{product?.name.en_US}
												</span>

												<small className="finance-dashboard-secondary-text mb-0 pb-0 text-capitalize text-muted">
													{`${quantity} ${
														skuOption?.skuOptionValueKey ||
														skuOption?.skuOptionValueName
													} license`}
												</small>
											</span>
										</div>
									</div>
								);
							},
							title: i18n.translate('app-name'),
						},
						{
							key: 'id',
							render: () =>
								textWrapper(
									product?.productSpecifications?.find(
										(specification) =>
											specification.specificationKey ===
											ProductSpecificationKey.APP_DEVELOPER_NAME
									)?.value.en_US || ''
								),
							title: i18n.translate('publisher'),
						},
						{
							key: 'finalPrice',
							render: (value) =>
								textWrapper(
									formatCurrency(
										value as number,
										currencyCode
									)
								),
							title: i18n.translate('net-price'),
						},
						{
							key: 'finalPriceWithTaxAmount',
							render: (value, order) =>
								textWrapper(
									formatCurrency(
										(value as number) -
											(order as {finalPrice?: number})
												?.finalPrice!,
										currencyCode
									)
								),
							title: i18n.translate('vat'),
						},
						{
							key: 'finalPriceWithTaxAmount',
							render: (value) =>
								textWrapper(
									formatCurrency(
										value as number,
										currencyCode
									)
								),
							title: i18n.translate('total'),
						},
					]}
					hasHover={false}
					rows={order?.orderItems || []}
				/>
			</DetailedCard>
		</PageRenderer>
	);
};

export default OrderDetails;
