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
import DetailHeader from '~/pages/MyAccount/Projects/components/DetailHeader/DetailHeader';
import DownloadListCard from '~/pages/MyAccount/Projects/components/DownloadListCard/DownloadListCard';
import EnvironmentCard from '~/pages/MyAccount/Projects/components/EnvironmentCard/EnvironmentCard';
import HelpSupportCard from '~/pages/MyAccount/Projects/components/HelpSupportCard/HelpSupportCard';
import LearnLinkCard from '~/pages/MyAccount/Projects/components/LearnLinkCard/LearnLinkCard';
import OrdersCard from '~/pages/MyAccount/Projects/components/OrdersCard/OrdersCard';
import ProjectDetailTabs, {
	DetailTab,
} from '~/pages/MyAccount/Projects/components/ProjectDetailTabs/ProjectDetailTabs';
import SectionedDetailsCard from '~/pages/MyAccount/Projects/components/SectionedDetailsCard/SectionedDetailsCard';
import UtilizationCard from '~/pages/MyAccount/Projects/components/UtilizationCard/UtilizationCard';
import {ProjectItemKind, ProjectTabKey} from '~/pages/MyAccount/Projects/types';
import {buildDetailsSections} from '~/pages/MyAccount/Projects/utils/buildDetailsSections';
import {buildEnvironmentSections} from '~/pages/MyAccount/Projects/utils/buildEnvironmentSections';
import {buildUtilizationSections} from '~/pages/MyAccount/Projects/utils/buildUtilizationSections';
import {PROJECT_TAB_LABELS} from '~/pages/MyAccount/Projects/utils/constants';
import {detailsMockData} from '~/pages/MyAccount/Projects/utils/detailsMockData';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';
import {resolveProductTabConfig} from '~/pages/MyAccount/Projects/utils/resolveProductTabConfig';
import {Liferay} from '~/services/liferay/liferay';

import ActivationPanel from '../components/ActivationPanel/ActivationPanel';

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

	const {
		activationProfile,
		detailsProfile,
		environmentProfile,
		learnUrl,
		tabKeys,
		utilizationProfile,
	} = resolveProductTabConfig({
		kind,
		orderType: orderInfo.orderType,
		product,
	});

	const detailsSections = buildDetailsSections(detailsProfile, {
		accountName: Liferay.CommerceContext.account?.accountName ?? '',
		mock: detailsMockData,
		orderInfo,
	});

	const tabContent: Record<ProjectTabKey, ReactNode> = {
		'activation': (
			<ActivationPanel
				product={product}
				profile={activationProfile ?? 'none'}
			/>
		),
		'details': <SectionedDetailsCard sections={detailsSections} />,
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
		'environment': environmentProfile ? (
			<SectionedDetailsCard
				icon="cloud"
				sections={buildEnvironmentSections(
					environmentProfile,
					orderInfo.environment
				)}
				title="workspace-info"
			/>
		) : (
			<EnvironmentCard environment={orderInfo.environment} />
		),
		'help-and-support': learnUrl ? (
			<LearnLinkCard url={learnUrl} />
		) : (
			<HelpSupportCard specifications={product.productSpecifications} />
		),
		'orders': <OrdersCard />,
		'utilization':
			utilizationProfile === 'usage-metrics' ? (
				<SectionedDetailsCard
					icon="analytics"
					sections={buildUtilizationSections(utilizationProfile)}
					title="usage"
				/>
			) : (
				<UtilizationCard />
			),
	};

	const tabs: DetailTab[] = tabKeys.map((tabKey) => ({
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
