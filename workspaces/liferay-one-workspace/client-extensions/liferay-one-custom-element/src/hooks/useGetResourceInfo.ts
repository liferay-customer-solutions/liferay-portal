/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import {ProductSpecificationKey} from '~/enums/Product';
import {Liferay} from '~/services/liferay/liferay';
import Console, {ConsoleUserProject} from '~/services/spring-boot/Console';
import {convertSize} from '~/utils/fileUtils';
import {getProductSpecificationValue} from '~/utils/productUtils';

import type {DeliveryProduct} from '~/types/product';

const INSUFFICIENT_RESOURCES = 0;

const checkResources = (
	product: DeliveryProduct,
	project?: ConsoleUserProject
) => {
	if (!project) {
		return false;
	}

	const instancesAvailable =
		project.rootProjectPlanUsage.instance.limit -
			project.rootProjectPlanUsage?.instance.used >
		INSUFFICIENT_RESOURCES;

	if (!instancesAvailable) {
		return false;
	}

	const compareResource = (
		key: keyof ConsoleUserProject['rootProjectPlanUsage'],
		resource: number | string
	) => {
		const limit = project?.rootProjectPlanUsage?.[key].limit ?? 0;
		const used = project?.rootProjectPlanUsage?.[key].used ?? 0;

		return limit - used > Number(resource);
	};

	const cpu = getProductSpecificationValue(
		ProductSpecificationKey.APP_BUILD_NUMBER_OF_CPUS,
		product
	);

	const ram = getProductSpecificationValue(
		ProductSpecificationKey.APP_BUILD_RAM_IN_GBS,
		product,
		'0'
	);

	return (
		compareResource('cpu', cpu) &&
		compareResource('memory', convertSize(ram, 'GB', 'MB'))
	);
};

const useGetResourceInfo = ({
	product,
	selectedProject,
	shouldFetch,
}: {
	product?: DeliveryProduct;
	selectedProject?: string;
	shouldFetch: boolean;
}) => {
	const {data: productUsages, isLoading} = useSWR(
		shouldFetch
			? `/product-usages/${Liferay.ThemeDisplay.getUserEmailAddress()}`
			: null,
		() => Console.getProjectsUsage()
	);

	const project = productUsages?.userProjects.find(
		(userProject) => userProject.rootProjectId === selectedProject
	);

	return {
		hasConsoleProjectsAvailable: !shouldFetch
			? true
			: Boolean(productUsages?.userProjects.length) && !isLoading,
		hasResources: checkResources(product as DeliveryProduct, project),
		isLoading,
		project,
		resourceRequest: productUsages,
	};
};

export default useGetResourceInfo;
