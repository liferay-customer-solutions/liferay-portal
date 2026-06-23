/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useNavigate} from 'react-router-dom';
import {KeyedMutator} from 'swr';
import ListView from '~/components/ListView/ListView';
import Page from '~/components/Page/Page';
import PaymentStatus from '~/components/PaymentStatusBadge/PaymentStatusBadge';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import PublisherSalesSummaries from '~/services/objects/PublisherSalesSummaries';
import {
	PaymentStatus as PaymentStatusCode,
	getTotalByOrderKey,
} from '~/utils/orderUtils';

import type {PublisherSalesSummaryEntry} from '~/types/publisher';

export const PublisherPayoutStatus = {
	PAID: 'paid',
	UNPAID: 'unpaid',
} as const;

export type PublisherPayoutStatus =
	(typeof PublisherPayoutStatus)[keyof typeof PublisherPayoutStatus];

async function onClickMarkAsPaid(
	entry: PublisherSalesSummaryEntry,
	mutate: KeyedMutator<unknown>
) {
	try {
		await PublisherSalesSummaries.patchPublisherSalesSummary(
			{
				paidBy: Liferay.ThemeDisplay.getUserName(),
				paidDate: new Date().toISOString(),
				paymentStatus: {
					key: PublisherPayoutStatus.PAID,
				},
			},
			entry.id
		);

		mutate((response: PublisherSalesSummaryEntry) => response, {
			revalidate: true,
		});

		Liferay.Util.openToast({
			message: i18n.translate('marked-as-paid'),
			type: 'success',
		});
	}
	catch {
		Liferay.Util.openToast({
			message: i18n.translate('oops-something-went-wrong'),
			type: 'danger',
		});
	}
}

const Payments = () => {
	const navigate = useNavigate();

	return (
		<Page
			description={i18n.translate('section-that-shows-the-payments')}
			pageRendererProps={{
				className: 'border mb-8 py-2 rounded-lg',
			}}
			title={i18n.translate('payments')}
		>
			<ListView<PublisherSalesSummaryEntry>
				emptyStateProps={{title: i18n.translate('no-orders-yet')}}
				id="finance-dashboard-orders"
				managementToolbarProps={{
					filterSchema: 'financeDashboardPayments',
					searchVisible: true,
					visible: true,
				}}
				resource="/o/c/publishersalessummaries?nestedFields=accountEntryToPublisher,publisherToCommerceOrder"
				tableProps={{
					actions: [
						{
							name: i18n.translate('view-details'),
							onClick: (entry) =>
								navigate('/mp-payments/' + entry.id),
						},
						{
							disabled: (entry) =>
								entry.paymentStatus.key ===
								PublisherPayoutStatus.PAID,
							name: i18n.translate('mark-as-paid'),
							onClick: onClickMarkAsPaid,
						},
					],
					columns: [
						{
							clickable: true,
							id: 'publisherName',
							name: i18n.translate('publisher'),
							render: (publisherName) => <b>{publisherName}</b>,
						},
						{
							id: 'quarter',
							name: i18n.translate('quarter'),
						},
						{
							id: 'publisherToCommerceOrder',
							name: i18n.translate('apps-sold'),
							render: (publisherToCommerceOrder) =>
								publisherToCommerceOrder?.length,
						},
						{
							id: 'publisherToCommerceOrder',
							name: i18n.translate('total'),
							render: (publisherToCommerceOrder) =>
								getTotalByOrderKey(
									'totalAmount',
									publisherToCommerceOrder
								),
						},
						{
							id: 'publisherToCommerceOrder',
							name: i18n.translate('net-price'),
							render: (publisherToCommerceOrder) =>
								getTotalByOrderKey(
									'subtotalAmount',
									publisherToCommerceOrder
								),
						},
						{
							id: 'publisherToCommerceOrder',
							name: i18n.translate('mp-commission'),
							render: (publisherToCommerceOrder) =>
								getTotalByOrderKey(
									'subtotalAmount',
									publisherToCommerceOrder,
									0.2
								),
						},
						{
							id: 'publisherToCommerceOrder',
							name: i18n.translate('publisher-payout'),
							render: (publisherToCommerceOrder) =>
								getTotalByOrderKey(
									'subtotalAmount',
									publisherToCommerceOrder,
									0.8
								),
						},
						{
							id: 'paymentStatus',
							name: i18n.translate('status'),
							render: (paymentStatus) => (
								<PaymentStatus
									paymentStatus={
										paymentStatus?.key ===
										PublisherPayoutStatus.PAID
											? PaymentStatusCode.PAID
											: PaymentStatusCode.PENDING
									}
								/>
							),
						},
					],
					navigateTo: (entry) => `/mp-payments/${entry.id}`,
				}}
			/>
		</Page>
	);
};

export default Payments;
