/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {format} from 'date-fns';
import {useMemo} from 'react';
import useSWR from 'swr';
import {useFetch} from '~/hooks/useFetch';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/projects';
import HeadlessCommerceDeliveryCatalog from '~/services/headless/HeadlessCommerceDeliveryCatalog';
import {Liferay} from '~/services/liferay/liferay';

import type {APIResponse} from '~/types/api';
import type {
	DeliveryProduct,
	DeliveryProductSpecification,
} from '~/types/product';

export type ProjectContract = {
	endDate?: string;
	externalReferenceCode: string;
	name: string;
	spendLimit?: number;
	startDate?: string;
	status?: string;
	termMonths?: number;
};

export type ProjectProduct = {
	categoryNames: string[];
	description: string;
	externalReferenceCode: string;
	id: string;
	name: string;
	publisher: string;
	saleType: string;
	specifications: DeliveryProductSpecification[];
	startDate: string;
	status: string;
	type: string;
};

type EntitlementNode = {
	endDate?: string;
	entitlementDefinitionToEntitlement?: {
		commerceProductToEntitlementDefinitionERC?: string;
		displayName?: string;
	};
	externalReferenceCode: string;
	name: string;
	startDate?: string;
};

type ContractNode = {
	contractTerm?: number;
	contractToEntitlement?: EntitlementNode[];
	customStatus?: string;
	endDate?: string;
	externalReferenceCode: string;
	name: string;
	r_projectToContract_c_projectId?: number;
	spendLimit?: number;
	startDate?: string;
};

type ProjectNode = {
	projectToContract?: ContractNode[];
};

type ProductEntitlement = {
	endDate?: string;
	productExternalReferenceCode?: string;
	startDate?: string;
};

function toProductEntitlements(
	contractNode?: ContractNode
): ProductEntitlement[] {
	return (contractNode?.contractToEntitlement ?? [])
		.map((entitlement) => ({
			endDate: entitlement.endDate,
			productExternalReferenceCode:
				entitlement.entitlementDefinitionToEntitlement
					?.commerceProductToEntitlementDefinitionERC,
			startDate: entitlement.startDate,
		}))
		.filter((entitlement) => entitlement.productExternalReferenceCode);
}

export function getSpecificationValue(
	product: DeliveryProduct,
	key: string
): string {
	return (
		(product.productSpecifications ?? []).find(
			(specification) => specification.specificationKey === key
		)?.value ?? ''
	);
}

export function getSpecificationValues(
	product: DeliveryProduct,
	key: string
): string[] {
	return (product.productSpecifications ?? [])
		.filter((specification) => specification.specificationKey === key)
		.map((specification) => specification.value);
}

function useChannelProducts() {
	const channelId = Liferay.CommerceContext.commerceChannelId;

	return useSWR(`/project-channel-products/${channelId}`, () =>
		HeadlessCommerceDeliveryCatalog.getProductsPage(
			channelId,
			new URLSearchParams({
				'accountId': '-1',
				'images.accountId': '-1',
				'nestedFields': 'categories,images,productSpecifications,skus',
				'pageSize': '100',
				'skus.accountId': '-1',
				'skus.currencyCode':
					Liferay.CommerceContext.currency.currencyCode,
			})
		)
	);
}

export function useProjectCommerce(projectExternalReferenceCode: string) {
	const {
		data,
		error,
		isLoading: loading,
	} = useFetch<ProjectNode>(
		projectExternalReferenceCode
			? `/o/c/projects/by-external-reference-code/${projectExternalReferenceCode}`
			: null,
		{
			params: {
				nestedFields:
					'projectToContract,contractToEntitlement,entitlementDefinitionToEntitlement',
				nestedFieldsDepth: 5,
			},
		}
	);

	const contractNode = data?.projectToContract?.[0];

	const contract: ProjectContract | undefined = contractNode && {
		endDate: contractNode.endDate,
		externalReferenceCode: contractNode.externalReferenceCode,
		name: contractNode.name,
		spendLimit: contractNode.spendLimit,
		startDate: contractNode.startDate,
		status: contractNode.customStatus,
		termMonths: contractNode.contractTerm,
	};

	const entitlements = toProductEntitlements(contractNode);

	return {contract, entitlements, error, loading};
}

export function useUnassignedCommerce(enabled = true) {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {
		data,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<ContractNode>>(
		enabled && accountId ? '/o/c/contracts' : null,
		{
			params: {
				filter: `r_accountEntryToContract_accountEntryId eq '${accountId}'`,
				nestedFields:
					'contractToEntitlement,entitlementDefinitionToEntitlement',
				nestedFieldsDepth: 5,
				pageSize: 100,
			},
		}
	);

	const entitlements = (data?.items ?? [])
		.filter((contract) => !contract.r_projectToContract_c_projectId)
		.flatMap((contract) => toProductEntitlements(contract));

	return {entitlements, error, loading};
}

export function useProjectProducts(projectExternalReferenceCode: string) {
	const unassigned = isUnassignedProject(projectExternalReferenceCode);

	const {
		contract,
		entitlements: projectEntitlements,
		error: projectError,
		loading: projectLoading,
	} = useProjectCommerce(unassigned ? '' : projectExternalReferenceCode);

	const {
		entitlements: unassignedEntitlements,
		error: unassignedError,
		loading: unassignedLoading,
	} = useUnassignedCommerce(unassigned);

	const entitlements = unassigned
		? unassignedEntitlements
		: projectEntitlements;

	const commerceError = unassigned ? unassignedError : projectError;
	const commerceLoading = unassigned ? unassignedLoading : projectLoading;

	const {
		data: productsData,
		error: productsError,
		isLoading: productsLoading,
	} = useChannelProducts();

	const products = useMemo<ProjectProduct[]>(() => {
		const productsByExternalReferenceCode = new Map(
			(productsData?.items ?? []).map((product) => [
				product.externalReferenceCode,
				product,
			])
		);

		return entitlements
			.map((entitlement) => {
				const product = productsByExternalReferenceCode.get(
					entitlement.productExternalReferenceCode as string
				);

				if (!product) {
					return null;
				}

				const categoryNames = (product.categories ?? []).map(
					(category) => category.name
				);

				return {
					categoryNames,
					description: product.description,
					externalReferenceCode: product.externalReferenceCode,
					id: String(product.productId ?? product.id),
					name: product.name,
					publisher: getSpecificationValue(product, 'publisher-name'),
					saleType: getSpecificationValue(product, 'price-model'),
					specifications: product.productSpecifications ?? [],
					startDate: entitlement.startDate
						? format(new Date(entitlement.startDate), 'MMM d, yyyy')
						: '',
					status: 'active',
					type:
						getSpecificationValues(
							product,
							'liferay-products-categories'
						)[0] ?? getSpecificationValue(product, 'price-model'),
				};
			})
			.filter((product): product is ProjectProduct => Boolean(product));
	}, [entitlements, productsData]);

	return {
		contract: unassigned ? undefined : contract,
		error: commerceError ?? productsError,
		loading: commerceLoading || productsLoading,
		products,
	};
}
