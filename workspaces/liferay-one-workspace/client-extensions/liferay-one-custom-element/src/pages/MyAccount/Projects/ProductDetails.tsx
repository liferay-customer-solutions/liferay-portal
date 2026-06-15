/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useParams} from 'react-router-dom';

import i18n from '../../../i18n';
import ActivationKeysCard from './ActivationKeysCard';
import DetailHeader from './DetailHeader';
import DetailsCard from './DetailsCard';
import DownloadListCard from './DownloadListCard';
import InvoicesCard from './InvoicesCard';
import OrdersCard from './OrdersCard';
import ProjectDetailTabs, {DetailTab} from './ProjectDetailTabs';
import {getProduct} from './products';
import {BUNDLES} from './tabData';

export default function ProductDetails() {
	const {productId} = useParams();

	const product = getProduct(productId ?? '');

	if (!product) {
		return (
			<ProjectDetailTabs
				header={
					<p className="text-neutral-7">
						{i18n.translate('no-results-found')}
					</p>
				}
				tabs={[]}
			/>
		);
	}

	const detailsContent = (
		<DetailsCard
			rows={[
				{
					label: i18n.translate('type'),
					value: i18n.translate(product.type),
				},
				{label: i18n.translate('order-id'), value: product.orderId},
				{
					label: i18n.translate('order-date'),
					value: product.orderDate,
				},
				{
					label: i18n.translate('purchased-by'),
					value: product.purchasedBy,
				},
				{
					label: i18n.translate('purchase-number'),
					value: product.purchaseNumber,
				},
				{
					label: i18n.translate('customer-account'),
					value: product.customerAccount,
				},
			]}
		/>
	);

	const tabs: DetailTab[] = [
		{content: detailsContent, key: 'details', label: 'details'},
	];

	if (product.type !== 'add-on') {
		tabs.push(
			{
				content: <ActivationKeysCard />,
				key: 'activation',
				label: 'activation',
			},
			{
				content: (
					<DownloadListCard
						emptyLabel="no-bundles-yet"
						heading="bundle-name"
						items={BUNDLES}
						title="bundle-list"
					/>
				),
				key: 'download',
				label: 'download',
			}
		);
	}

	tabs.push(
		{content: <OrdersCard />, key: 'orders', label: 'orders'},
		{content: <InvoicesCard />, key: 'invoices', label: 'invoices'}
	);

	return (
		<ProjectDetailTabs
			header={
				<DetailHeader
					description={product.description}
					icon={product.icon}
					logoColor={product.logoColor}
					name={product.name}
					publisher={product.publisher}
					showByPrefix
					status={product.status}
				/>
			}
			tabs={tabs}
		/>
	);
}
