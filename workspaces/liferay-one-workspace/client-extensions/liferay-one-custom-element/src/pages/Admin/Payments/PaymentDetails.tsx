/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayTable from '@clayui/table';
import {useParams} from 'react-router-dom';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {PageRenderer} from '~/components/Page/Page';
import PaymentDetailsHeader from '~/components/PaymentDetailsHeader/PaymentDetailsHeader';
import PaymentStatusBadge from '~/components/PaymentStatusBadge/PaymentStatusBadge';
import QATable, {Orientation} from '~/components/QATable/QATable';
import Table from '~/components/Table/Table';
import usePublisherSalesSummaryObject from '~/hooks/usePublisherSalesSummaryObject';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import PublisherSalesSummaries from '~/services/objects/PublisherSalesSummaries';
import {formatDateTime} from '~/utils/dateUtils';
import {exportToCSV} from '~/utils/exportToCSV';
import {formatCurrency} from '~/utils/formatCurrency';
import {
	PaymentStatus as PaymentStatusCode,
	getTotalByOrderKey,
} from '~/utils/orderUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import {PublisherPayoutStatus} from './Payments';

import type {AccountPostalAddresses} from '~/types/accounts';

function textWrapper(content: string | number | undefined) {
	if (content === undefined || content === null || content === '') {
		return <p className="mb-2 mt-1">-</p>;
	}

	return <p className="mb-2 mt-1">{content}</p>;
}

const Payouts = {
	MP_COMMISSION: 0.2,
	PUBLISHER_PAYOUT: 0.8,
} as const;

export function formatPostalAddress(
	address: AccountPostalAddresses | undefined
) {
	if (!address || !Object.keys(address).length) {
		return '-';
	}

	return [
		address.streetAddressLine1,
		address.addressLocality,
		address.addressRegion,
		address.postalCode,
		address.addressCountry,
	]
		.filter(Boolean)
		.join(', ');
}

const PaymentDetails = () => {
	const {entryId} = useParams();

	const {data, error, isLoading, mutate} = usePublisherSalesSummaryObject(
		entryId as string
	);

	const {
		account,
		completeOrderItems,
		postalAddresses = {items: []},
		publisherSalesSummary,
	} = data || {};

	const postalAddress = postalAddresses.items.find(
		(address) => address.addressType === 'billing'
	);

	const emailAddress = account?.customFields?.find(
		(customField) => customField.name === 'Contact Email'
	)?.customValue?.data;

	const paymentStatus = publisherSalesSummary?.paymentStatus.key;

	const paymentStatusCode =
		paymentStatus === PublisherPayoutStatus.PAID
			? PaymentStatusCode.PAID
			: PaymentStatusCode.PENDING;

	function exportOrdersCSV() {
		if (!completeOrderItems?.length) {
			return;
		}

		const _formatNumber = (price: number): string => {
			if (!price) {
				return '0.00';
			}

			return price.toLocaleString(undefined, {
				maximumFractionDigits: 2,
				minimumFractionDigits: 2,
			});
		};

		const headers = [
			i18n.translate('app-name'),
			i18n.translate('account'),
			i18n.translate('quantity'),
			i18n.translate('net-price'),
			i18n.translate('vat'),
			i18n.translate('total'),
			i18n.translate('currency-code'),
		];

		const rows = completeOrderItems.map(({orderItem, placedOrderItem}) => {
			const finalPrice = orderItem.finalPrice ?? 0;
			const finalPriceWithTax = orderItem.finalPriceWithTaxAmount ?? 0;
			const vat = finalPriceWithTax - finalPrice;

			return [
				placedOrderItem.name || '',
				orderItem.account?.name || '',
				placedOrderItem.quantity,
				_formatNumber(finalPrice),
				_formatNumber(vat),
				_formatNumber(finalPriceWithTax),
				orderItem.currencyCode,
			];
		});

		exportToCSV(`orders.${Date.now()}.csv`, headers, rows);
	}

	return (
		<PageRenderer
			className="app-details-header d-flex flex-column w-100"
			error={error}
			isLoading={isLoading}
		>
			<PaymentDetailsHeader
				backLink="/mp-payments"
				onClick={() =>
					PublisherSalesSummaries.patchPublisherSalesSummary(
						{
							paidBy: Liferay.ThemeDisplay.getUserName(),
							paidDate: new Date().toISOString(),
							paymentStatus: {
								key: PublisherPayoutStatus.PAID,
							},
						},
						entryId!
					).then((updatedPublisherSalesSummary) => {
						mutate(
							{
								...data!,
								publisherSalesSummary: {
									...data!.publisherSalesSummary,
									...updatedPublisherSalesSummary,
								},
							},
							{revalidate: false}
						);
					})
				}
				paymentStatusCode={paymentStatusCode}
				showButton={paymentStatus !== PublisherPayoutStatus.PAID}
				title={publisherSalesSummary?.publisherName as string}
			/>

			<div className="d-flex mt-5">
				<DetailedCard
					cardIconAltText="order-form-pencil"
					cardTitle={i18n.translate('publisher-details')}
					className="mr-5 w-100"
					clayIcon="order-form-pencil"
				>
					<QATable
						items={[
							{
								title: i18n.translate('publisher-name'),
								value: textWrapper(account?.name),
							},
							{
								title: i18n.translate('email'),
								value: textWrapper(emailAddress as string),
							},
							{
								title: i18n.translate('billing-address'),
								value: textWrapper(
									formatPostalAddress(postalAddress)
								),
							},
							{
								title: i18n.translate('vat-number'),
								value: textWrapper(account?.taxId),
							},
							{
								title: i18n.translate('quarter'),
								value: textWrapper(
									publisherSalesSummary?.quarter
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>

				<DetailedCard
					cardIconAltText="paste-plaintext"
					cardTitle={i18n.translate('payment-summary')}
					className="w-100"
					clayIcon="paste-plaintext"
				>
					<QATable
						items={[
							{
								title: i18n.translate('apps-sold'),
								value: textWrapper(
									publisherSalesSummary
										?.publisherToCommerceOrder?.length
								),
							},
							{
								title: i18n.translate('net-price'),
								value: textWrapper(
									getTotalByOrderKey(
										'subtotalAmount',
										publisherSalesSummary?.publisherToCommerceOrder!
									)
								),
							},
							{
								title: i18n.translate('total'),
								value: textWrapper(
									getTotalByOrderKey(
										'totalAmount',
										publisherSalesSummary?.publisherToCommerceOrder!
									)
								),
							},
							{
								title: i18n.translate('mp-commission'),
								value: textWrapper(
									getTotalByOrderKey(
										'totalAmount',
										publisherSalesSummary?.publisherToCommerceOrder!,
										Payouts.MP_COMMISSION
									)
								),
							},
							{
								title: i18n.translate('publisher-payout'),
								value: textWrapper(
									getTotalByOrderKey(
										'totalAmount',
										publisherSalesSummary?.publisherToCommerceOrder!,
										Payouts.PUBLISHER_PAYOUT
									)
								),
							},
							{
								title: i18n.translate('status'),
								value: (
									<PaymentStatusBadge
										paymentStatus={paymentStatusCode}
									/>
								),
							},
							{
								className: 'mt-2',
								title: i18n.translate('paid-date'),
								value: textWrapper(
									formatDateTime(
										publisherSalesSummary?.paidDate
									)
								),
								visible:
									paymentStatus ===
									PublisherPayoutStatus.PAID,
							},
							{
								title: i18n.translate('paid-by'),
								value: textWrapper(
									publisherSalesSummary?.paidBy
								),
								visible:
									paymentStatus ===
									PublisherPayoutStatus.PAID,
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>
			</div>

			<DetailedCard
				cardIconAltText="order-form"
				cardTitle={i18n.translate('apps-sold')}
				className="mt-5 pb-0 w-100"
				clayIcon="price-tag"
				headerActions={
					<ClayButton
						className="export-csv-button"
						disabled={!completeOrderItems?.length}
						displayType="unstyled"
						onClick={exportOrdersCSV}
					>
						<ClayIcon
							aria-hidden="true"
							className="inline-item inline-item-before"
							symbol="download"
						/>

						<span className="font-weight-semi-bold">
							{i18n.translate('export-csv')}
						</span>
					</ClayButton>
				}
			>
				<Table
					className="table-borderless"
					columns={[
						{
							key: 'placedOrderItem',
							render: (value) => {
								const placedOrderItem = value as {
									name?: string;
									options?: string;
									quantity?: number;
									thumbnail?: string;
								};
								const [skuOption] = safeJSONParse(
									placedOrderItem.options ?? null,
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
												src={placedOrderItem.thumbnail}
											/>

											<span className="d-flex flex-column">
												<span className="font-weight-bold">
													{placedOrderItem.name}
												</span>

												<small className="finance-dashboard-secondary-text mb-0 pb-0 text-capitalize text-muted">
													{`${placedOrderItem.quantity} ${
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
							key: 'placedOrderItem',
							render: (value, row) => {
								const placedOrderItem = value as {
									author?: string;
								};
								const order = row as {
									orderItem?: {account?: {name?: string}};
								};

								return (
									<div className="d-flex flex-column justify-content-center">
										<p className="mb-0">
											{order.orderItem?.account?.name}
										</p>
										<p className="finance-dashboard-secondary-text mb-0">
											{placedOrderItem.author}
										</p>
									</div>
								);
							},
							title: i18n.translate('account'),
						},
						{
							key: 'orderItem',
							render: (value) => {
								const orderItem = value as {
									currencyCode?: string;
									finalPrice?: number;
								};

								return formatCurrency(
									orderItem.finalPrice ?? 0,
									orderItem.currencyCode
								);
							},
							title: i18n.translate('net-price'),
						},
						{
							key: 'orderItem',
							render: (value) => {
								const orderItem = value as {
									currencyCode?: string;
									finalPrice?: number;
									finalPriceWithTaxAmount?: number;
								};

								return formatCurrency(
									(orderItem.finalPriceWithTaxAmount ?? 0) -
										(orderItem.finalPrice ?? 0),
									orderItem.currencyCode
								);
							},
							title: i18n.translate('vat'),
						},
						{
							key: 'orderItem',
							render: (value) => {
								const orderItem = value as {
									currencyCode?: string;
									finalPriceWithTaxAmount?: number;
								};

								return formatCurrency(
									orderItem?.finalPriceWithTaxAmount ?? 0,
									orderItem.currencyCode
								);
							},
							title: i18n.translate('total'),
						},
					]}
					hasHover={false}
					rows={completeOrderItems || []}
				>
					<ClayTable.Row className="publisher-payout-footer w-100">
						<ClayTable.Cell colSpan={2}>
							<b>{i18n.translate('total')}</b>
						</ClayTable.Cell>

						<ClayTable.Cell>
							{getTotalByOrderKey(
								'subtotalAmount',
								publisherSalesSummary?.publisherToCommerceOrder!
							)}
						</ClayTable.Cell>

						<ClayTable.Cell>
							{getTotalByOrderKey(
								'taxAmountValue',
								publisherSalesSummary?.publisherToCommerceOrder!
							)}
						</ClayTable.Cell>

						<ClayTable.Cell>
							{getTotalByOrderKey(
								'totalAmount',
								publisherSalesSummary?.publisherToCommerceOrder!
							)}
						</ClayTable.Cell>
					</ClayTable.Row>
				</Table>
			</DetailedCard>
		</PageRenderer>
	);
};

export default PaymentDetails;
