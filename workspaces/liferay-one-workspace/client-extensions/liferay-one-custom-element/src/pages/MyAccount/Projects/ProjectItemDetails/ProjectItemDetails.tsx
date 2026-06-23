/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import {useParams} from 'react-router-dom';
import {useProject} from '~/context/ProjectContext';
import {useDeliveryProduct} from '~/hooks/useDeliveryProduct';
import {useLiferayBundles} from '~/hooks/useLiferayBundles';
import {
	getSpecificationValue,
	getSpecificationValues,
	useProjectProducts,
} from '~/hooks/useProjectCommerce';
import {getProductOrderInfo, useProjectOrders} from '~/hooks/useProjectOrders';
import i18n from '~/i18n';
import ActivationKeysCard from '~/pages/MyAccount/Projects/components/ActivationKeysCard/ActivationKeysCard';
import DetailHeader from '~/pages/MyAccount/Projects/components/DetailHeader/DetailHeader';
import DetailsCard, {
	DetailsRow,
} from '~/pages/MyAccount/Projects/components/DetailsCard/DetailsCard';
import DownloadListCard from '~/pages/MyAccount/Projects/components/DownloadListCard/DownloadListCard';
import EnvironmentCard from '~/pages/MyAccount/Projects/components/EnvironmentCard/EnvironmentCard';
import HelpSupportCard from '~/pages/MyAccount/Projects/components/HelpSupportCard/HelpSupportCard';
import OrdersCard from '~/pages/MyAccount/Projects/components/OrdersCard/OrdersCard';
import ProjectDetailTabs, {
	DetailTab,
} from '~/pages/MyAccount/Projects/components/ProjectDetailTabs/ProjectDetailTabs';
import UtilizationCard from '~/pages/MyAccount/Projects/components/UtilizationCard/UtilizationCard';
import {ProjectItemKind, ProjectTabKey} from '~/pages/MyAccount/Projects/types';
import {PROJECT_TAB_LABELS} from '~/pages/MyAccount/Projects/utils/constants';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';
import {getVisibleProjectTabKeys} from '~/pages/MyAccount/Projects/utils/getVisibleProjectTabKeys';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';
import {Liferay} from '~/services/liferay/liferay';

type ProjectItemDetailsProps = {
	kind: ProjectItemKind;
};

export default function ProjectItemDetails({kind}: ProjectItemDetailsProps) {
	const {applicationERC, productERC} = useParams();
	const {projectId, projects} = useProject();

	const itemERC = productERC ?? applicationERC ?? '';

	const projectName = isUnassignedProject(projectId)
		? undefined
		: projects.find(
				(project) => project.externalReferenceCode === projectId
			)?.name;

	const {loading: productsLoading, products} = useProjectProducts(projectId);

	const productId =
		products.find((product) => product.externalReferenceCode === itemERC)
			?.id ?? '';

	const {data: product, isLoading} = useDeliveryProduct(productId);
	const {placedOrders} = useProjectOrders(projectName);
	const {bundles} = useLiferayBundles();

	if (productsLoading || isLoading) {
		return (
			<ProjectDetailTabs
				header={
					<p className="text-neutral-7">
						{i18n.translate('loading')}
					</p>
				}
				tabs={[]}
			/>
		);
	}

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

	const type =
		getSpecificationValues(product, 'liferay-products-categories')[0] ??
		getSpecificationValue(product, 'price-model');

	const orderInfo = getProductOrderInfo(placedOrders, product.name);

	const detailsRows: DetailsRow[] = [
		...(kind === 'product'
			? [{label: i18n.translate('type'), value: type}]
			: []),
		{label: i18n.translate('order-id'), value: orderInfo.orderId},
		{label: i18n.translate('order-date'), value: orderInfo.orderDate},
		{label: i18n.translate('purchased-by'), value: orderInfo.purchasedBy},
		{
			label: i18n.translate('purchase-number'),
			value: orderInfo.purchaseNumber,
		},
		...(kind === 'application'
			? [
					{
						label: i18n.translate('project-type'),
						value:
							getSpecificationValue(product, 'license-term') ||
							type,
					},
				]
			: []),
		{
			label: i18n.translate('customer-account'),
			value: Liferay.CommerceContext.account?.accountName ?? '',
		},
	];

	const tabContent: Record<ProjectTabKey, ReactNode> = {
		'activation': <ActivationKeysCard productName={product.name} />,
		'details': <DetailsCard rows={detailsRows} />,
		'download': (
			<DownloadListCard
				emptyLabel={
					kind === 'product' ? 'no-bundles-yet' : 'no-versions-yet'
				}
				heading={
					kind === 'product' ? 'bundle-name' : 'supported-version'
				}
				items={bundles}
				title={kind === 'product' ? 'bundle-list' : 'versions-list'}
			/>
		),
		'environment': <EnvironmentCard environment={orderInfo.environment} />,
		'help-and-support': (
			<HelpSupportCard specifications={product.productSpecifications} />
		),
		'orders': <OrdersCard />,
		'utilization': <UtilizationCard />,
	};

	const tabs: DetailTab[] = getVisibleProjectTabKeys({
		kind,
		orderType: orderInfo.orderType,
		product,
	}).map((tabKey) => ({
		content: tabContent[tabKey],
		key: tabKey,
		label: PROJECT_TAB_LABELS[tabKey],
	}));

	return (
		<ProjectDetailTabs
			header={
				<DetailHeader
					description={
						kind === 'product' ? product.description : undefined
					}
					icon={kind === 'product' ? getProductIcon(type) : undefined}
					logoColor={getLogoColor(product.name)}
					name={product.name}
					publisher={getSpecificationValue(product, 'publisher-name')}
					showByPrefix={kind === 'product'}
					status="active"
				/>
			}
			tabs={tabs}
		/>
	);
}
