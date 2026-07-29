/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {format} from 'date-fns';
import {useMemo} from 'react';
import useSWR from 'swr';
import {useFetch} from '~/hooks/useFetch';
import {getProductContactRoleExternalReferenceCodes} from '~/pages/MyAccount/ProjectMembers/projectRoles';
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
	endDate: string;
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
	commerceOrderItemToEntitlement?: {
		orderExternalReferenceCode?: string;
	};
	endDate?: string;
	entitlementDefinitionToEntitlement?: {
		commerceProductToEntitlementDefinitionERC?: string;
		displayName?: string;
	};
	externalReferenceCode: string;
	name: string;
	r_projectToEntitlement_c_projectERC?: string;
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
	name?: string;
	projectToContract?: ContractNode[];
	projectToEntitlement?: EntitlementNode[];
};

type ProductEntitlement = {
	endDate?: string;
	orderExternalReferenceCode?: string;
	productExternalReferenceCode?: string;
	projectExternalReferenceCode?: string;
	startDate?: string;
};

function toProjectContract(contractNode: ContractNode): ProjectContract {
	return {
		endDate: contractNode.endDate,
		externalReferenceCode: contractNode.externalReferenceCode,
		name: contractNode.name,
		spendLimit: contractNode.spendLimit,
		startDate: contractNode.startDate,
		status: contractNode.customStatus,
		termMonths: contractNode.contractTerm,
	};
}

export function resolveDefaultContractERC(
	contracts: ProjectContract[]
): string | undefined {
	const activeContracts = contracts.filter(
		(contract) => contract.status === 'active'
	);

	const selectableContracts = activeContracts.length
		? activeContracts
		: contracts;

	if (!selectableContracts.length) {
		return undefined;
	}

	return selectableContracts.reduce((costliest, contract) =>
		(contract.spendLimit ?? 0) > (costliest.spendLimit ?? 0)
			? contract
			: costliest
	).externalReferenceCode;
}

function getEntitlementStatus(endDate?: string): string {
	if (endDate && new Date(endDate) < new Date()) {
		return 'expired';
	}

	return 'active';
}

function toProductEntitlements(
	entitlementNodes?: EntitlementNode[]
): ProductEntitlement[] {
	return (entitlementNodes ?? [])
		.map((entitlement) => ({
			endDate: entitlement.endDate,
			orderExternalReferenceCode:
				entitlement.commerceOrderItemToEntitlement
					?.orderExternalReferenceCode,
			productExternalReferenceCode:
				entitlement.entitlementDefinitionToEntitlement
					?.commerceProductToEntitlementDefinitionERC,
			projectExternalReferenceCode:
				entitlement.r_projectToEntitlement_c_projectERC,
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

export function useChannelProducts() {
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

export function useProjectCommerce(
	projectExternalReferenceCode: string,
	contractExternalReferenceCode?: string
) {
	const accountId = Liferay.CommerceContext?.account?.accountId;

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
					'projectToContract,projectToEntitlement,contractToEntitlement,entitlementDefinitionToEntitlement',
				nestedFieldsDepth: 3,
			},
		}
	);

	const {data: accountData, isLoading: accountLoading} = useFetch<
		APIResponse<ContractNode>
	>(projectExternalReferenceCode && accountId ? '/o/c/contracts' : null, {
		params: {
			filter: `r_accountEntryToContract_accountEntryId eq '${accountId}'`,
			nestedFields:
				'commerceOrderItemToEntitlement,contractToEntitlement,entitlementDefinitionToEntitlement',
			nestedFieldsDepth: 2,
			pageSize: 100,
		},
	});

	const projectContractNodes = data?.projectToContract ?? [];

	const accountContractNodes = (accountData?.items ?? []).filter(
		(contract) => !contract.r_projectToContract_c_projectId
	);

	const usingAccountFallback = !projectContractNodes.length;

	const contractNodes = usingAccountFallback
		? accountContractNodes
		: projectContractNodes;

	const contracts = contractNodes.map(toProjectContract);

	const selectedContractExists = contracts.some(
		(contract) =>
			contract.externalReferenceCode === contractExternalReferenceCode
	);

	const resolvedContractERC = selectedContractExists
		? contractExternalReferenceCode
		: resolveDefaultContractERC(contracts);

	const contractNode = contractNodes.find(
		(node) => node.externalReferenceCode === resolvedContractERC
	);

	const contract = contractNode ? toProjectContract(contractNode) : undefined;

	const entitlements = usingAccountFallback
		? toProductEntitlements(data?.projectToEntitlement)
		: toProductEntitlements(contractNode?.contractToEntitlement);

	return {
		contract,
		contracts,
		entitlements,
		error,
		loading: loading || accountLoading,
		projectName: data?.name,
		usingAccountFallback,
	};
}

function useAccountOrderEntitlements(enabled = true) {
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
					'commerceOrderItemToEntitlement,contractToEntitlement,entitlementDefinitionToEntitlement',
				nestedFieldsDepth: 2,
				pageSize: 100,
			},
		}
	);

	const entitlements = useMemo(
		() =>
			(data?.items ?? [])
				.filter((contract) => !contract.r_projectToContract_c_projectId)
				.flatMap((contract) =>
					toProductEntitlements(contract.contractToEntitlement)
				),
		[data]
	);

	return {entitlements, error, loading};
}

export function useUnassignedCommerce(enabled = true) {
	const {entitlements, error, loading} = useAccountOrderEntitlements(enabled);

	return {
		entitlements: entitlements.filter(
			(entitlement) => !entitlement.projectExternalReferenceCode
		),
		error,
		loading,
	};
}

export function useAccountProducts() {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {data: contractsData, isLoading: contractsLoading} = useFetch<
		APIResponse<ContractNode>
	>(accountId ? '/o/c/contracts' : null, {
		params: {
			filter: `r_accountEntryToContract_accountEntryId eq '${accountId}'`,
			nestedFields:
				'contractToEntitlement,entitlementDefinitionToEntitlement',
			nestedFieldsDepth: 2,
			pageSize: 100,
		},
	});

	const {data: productsData, isLoading: productsLoading} =
		useChannelProducts();

	const products = useMemo<DeliveryProduct[]>(() => {
		const productsByExternalReferenceCode = new Map(
			(productsData?.items ?? []).map((product) => [
				product.externalReferenceCode,
				product,
			])
		);

		const accountProducts = new Map<string, DeliveryProduct>();

		(contractsData?.items ?? []).forEach((contract) => {
			toProductEntitlements(contract.contractToEntitlement).forEach(
				(entitlement) => {
					const product = productsByExternalReferenceCode.get(
						entitlement.productExternalReferenceCode as string
					);

					if (product) {
						accountProducts.set(
							product.externalReferenceCode,
							product
						);
					}
				}
			);
		});

		return [...accountProducts.values()];
	}, [contractsData, productsData]);

	return {loading: contractsLoading || productsLoading, products};
}

export function useAccountProjectContactRoles() {
	const accountId = Liferay.CommerceContext?.account?.accountId;

	const {data: contractsData, isLoading: contractsLoading} = useFetch<
		APIResponse<ContractNode>
	>(accountId ? '/o/c/contracts' : null, {
		params: {
			filter: `r_accountEntryToContract_accountEntryId eq '${accountId}'`,
			nestedFields:
				'contractToEntitlement,entitlementDefinitionToEntitlement',
			nestedFieldsDepth: 2,
			pageSize: 100,
		},
	});

	const {data: productsData, isLoading: productsLoading} =
		useChannelProducts();

	const contactRoleExternalReferenceCodesByProjectId = useMemo(() => {
		const productsByExternalReferenceCode = new Map(
			(productsData?.items ?? []).map((product) => [
				product.externalReferenceCode,
				product,
			])
		);

		const externalReferenceCodesByProjectId = new Map<
			number,
			Set<string>
		>();

		(contractsData?.items ?? []).forEach((contract) => {
			const projectId = contract.r_projectToContract_c_projectId;

			if (!projectId) {
				return;
			}

			const externalReferenceCodes =
				externalReferenceCodesByProjectId.get(projectId) ??
				new Set<string>();

			toProductEntitlements(contract.contractToEntitlement).forEach(
				(entitlement) => {
					const product = productsByExternalReferenceCode.get(
						entitlement.productExternalReferenceCode as string
					);

					if (!product) {
						return;
					}

					getProductContactRoleExternalReferenceCodes(
						product.productSpecifications ?? []
					).forEach((externalReferenceCode) =>
						externalReferenceCodes.add(externalReferenceCode)
					);
				}
			);

			externalReferenceCodesByProjectId.set(
				projectId,
				externalReferenceCodes
			);
		});

		return new Map(
			[...externalReferenceCodesByProjectId].map(
				([projectId, externalReferenceCodes]) => [
					projectId,
					[...externalReferenceCodes],
				]
			)
		);
	}, [contractsData, productsData]);

	return {
		contactRoleExternalReferenceCodesByProjectId,
		loading: contractsLoading || productsLoading,
	};
}

export function useProjectProducts(
	projectExternalReferenceCode: string,
	contractExternalReferenceCode?: string
) {
	const unassigned = isUnassignedProject(projectExternalReferenceCode);

	const {
		contract,
		contracts,
		entitlements: projectEntitlements,
		error: projectError,
		loading: projectLoading,
	} = useProjectCommerce(
		unassigned ? '' : projectExternalReferenceCode,
		contractExternalReferenceCode
	);

	const {
		entitlements: accountOrderEntitlements,
		error: accountError,
		loading: accountLoading,
	} = useAccountOrderEntitlements(unassigned);

	const entitlements = useMemo(() => {
		if (unassigned) {
			return accountOrderEntitlements.filter(
				(entitlement) => !entitlement.projectExternalReferenceCode
			);
		}

		return projectEntitlements;
	}, [accountOrderEntitlements, projectEntitlements, unassigned]);

	const commerceError = projectError ?? accountError;
	const commerceLoading = projectLoading || accountLoading;

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
					endDate: entitlement.endDate
						? format(new Date(entitlement.endDate), 'MMM d, yyyy')
						: '',
					externalReferenceCode: product.externalReferenceCode,
					id: String(product.productId ?? product.id),
					name: product.name,
					publisher: getSpecificationValue(product, 'publisher-name'),
					saleType: getSpecificationValue(product, 'price-model'),
					specifications: product.productSpecifications ?? [],
					startDate: entitlement.startDate
						? format(new Date(entitlement.startDate), 'MMM d, yyyy')
						: '',
					status: getEntitlementStatus(entitlement.endDate),
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
		contracts: unassigned ? [] : contracts,
		error: commerceError ?? productsError,
		loading: commerceLoading || productsLoading,
		products,
	};
}
