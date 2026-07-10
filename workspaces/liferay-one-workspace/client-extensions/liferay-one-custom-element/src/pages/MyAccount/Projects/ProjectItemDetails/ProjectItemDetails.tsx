/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import {useParams} from 'react-router-dom';
import {useProject} from '~/context/ProjectContext';
import {useDeliveryProduct} from '~/hooks/useDeliveryProduct';
import {
	getSpecificationValue,
	getSpecificationValues,
	useProjectProducts,
} from '~/hooks/useProjectCommerce';
import {getProductOrderInfo, useProjectOrders} from '~/hooks/useProjectOrders';
import i18n, {Word} from '~/i18n';
import ActivationTab from '~/pages/MyAccount/Projects/components/ActivationTab/ActivationTab';
import DetailHeader from '~/pages/MyAccount/Projects/components/DetailHeader/DetailHeader';
import DetailsTab from '~/pages/MyAccount/Projects/components/DetailsTab/DetailsTab';
import DownloadTab from '~/pages/MyAccount/Projects/components/DownloadTab/DownloadTab';
import EnvironmentTab from '~/pages/MyAccount/Projects/components/EnvironmentTab/EnvironmentTab';
import HelpSupportTab from '~/pages/MyAccount/Projects/components/HelpSupportTab/HelpSupportTab';
import OrdersTab from '~/pages/MyAccount/Projects/components/OrdersTab/OrdersTab';
import ProjectDetailTabs, {
	DetailTab,
} from '~/pages/MyAccount/Projects/components/ProjectDetailTabs/ProjectDetailTabs';
import UtilizationTab from '~/pages/MyAccount/Projects/components/UtilizationTab/UtilizationTab';
import {ProjectItemKind, ProjectTabKey} from '~/pages/MyAccount/Projects/types';
import {PROJECT_TAB_LABELS} from '~/pages/MyAccount/Projects/utils/constants';
import {getLogoColor} from '~/pages/MyAccount/Projects/utils/getLogoColor';
import {getProductIcon} from '~/pages/MyAccount/Projects/utils/getProductIcon';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';
import {resolveProductTabConfig} from '~/pages/MyAccount/Projects/utils/resolveProductTabConfig';

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

	const {
		contract,
		loading: productsLoading,
		products,
	} = useProjectProducts(projectId);

	const productId =
		products.find((product) => product.externalReferenceCode === itemERC)
			?.id ?? '';

	const {data: product, isLoading} = useDeliveryProduct(productId);
	const {placedOrders} = useProjectOrders(projectName);

	const renderMessage = (word: Word) => (
		<ProjectDetailTabs
			header={<p className="text-neutral-7">{i18n.translate(word)}</p>}
			tabs={[]}
		/>
	);

	if (productsLoading || isLoading) {
		return renderMessage('loading');
	}

	if (!product) {
		return renderMessage('no-results-found');
	}

	const iconCategory =
		getSpecificationValues(product, 'liferay-products-categories')[0] ??
		getSpecificationValue(product, 'price-model');

	const orderInfo = getProductOrderInfo(placedOrders, product.name);

	const {activationProfile, detailsProfile, learnUrl, tabKeys} =
		resolveProductTabConfig({
			kind,
			orderType: orderInfo.orderType,
			product,
		});

	const tabContent: Record<ProjectTabKey, () => ReactNode> = {
		'activation': () => (
			<ActivationTab product={product} profile={activationProfile} />
		),
		'details': () => (
			<DetailsTab
				contract={contract}
				orderInfo={orderInfo}
				profile={detailsProfile}
			/>
		),
		'download': () => <DownloadTab kind={kind} />,
		'environment': () => (
			<EnvironmentTab environment={orderInfo.environment} />
		),
		'help-and-support': () => (
			<HelpSupportTab learnUrl={learnUrl} product={product} />
		),
		'orders': () => <OrdersTab />,
		'utilization': () => <UtilizationTab />,
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
					icon={
						kind === 'product'
							? getProductIcon(iconCategory)
							: undefined
					}
					logoColor={getLogoColor(product.name)}
					name={product.name}
					publisher={getSpecificationValue(product, 'publisher-name')}
					showByPrefix={kind === 'product'}
					status={orderInfo.status || 'active'}
				/>
			}
			tabs={tabs}
		/>
	);
}
