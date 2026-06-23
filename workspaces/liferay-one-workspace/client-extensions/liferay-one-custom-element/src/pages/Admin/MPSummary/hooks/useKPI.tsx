/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ComponentProps} from 'react';
import {useNavigate} from 'react-router-dom';
import useSWR from 'swr';
import {useOneContext} from '~/context/OneContextProvider';
import useListTypeDefinition from '~/hooks/useListTypeDefinition';
import useModalContext from '~/hooks/useModalContext';
import i18n from '~/i18n';
import ProjectsUsingMarketplaceModalBody from '~/pages/Admin/MPSummary/components/ProjectsUsingMarketplace';
import GraphQL from '~/services/headless/GraphQL';
import HeadlessCommerceAdminCatalog from '~/services/headless/HeadlessCommerceAdminCatalog';
import SearchBuilder from '~/utils/SearchBuilder';
import {ProductWorkflowStatusCode} from '~/utils/productUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

const currentYear = new Date().getFullYear();
const lastYear = currentYear - 1;
const lastYearISO = new Date(currentYear, 0, 1, 0, 0, 0).toISOString();

const baseSearchBuilder = new SearchBuilder()
	.in('statusCode', [ProductWorkflowStatusCode.APPROVED])
	.and();

const lastYearBaseSearchBuilder = new SearchBuilder()
	.lt('createDate', lastYearISO)
	.and()
	.in('statusCode', [ProductWorkflowStatusCode.APPROVED])
	.and();

const buildQReleaseFilter = (base: SearchBuilder) =>
	base
		.clone()
		.group('OPEN')
		.lambdaContains('specificationValues', '2026 Q')
		.or()
		.lambdaContains('specificationValues', '2025 Q')
		.or()
		.lambdaContains('specificationValues', '2024 Q')
		.or()
		.lambdaContains('specificationValues', '2023 Q')
		.group('CLOSE')
		.and()
		.not()
		.lambda('specificationValues', 'low-code-configuration')
		.build();

const appsAndConnectorSupportingQReleaseFilter =
	buildQReleaseFilter(baseSearchBuilder);

const lastYearAppsAndConnectorSupportingQReleaseFilter = buildQReleaseFilter(
	lastYearBaseSearchBuilder
);

const lowCodeConfigurationsPublishedFilter = baseSearchBuilder
	.clone()
	.lambda('specificationValues', 'low-code-configuration')
	.build();

const lastYearLowCodeConfigurationsPublishedFilter = lastYearBaseSearchBuilder
	.clone()
	.lambda('specificationValues', 'low-code-configuration')
	.build();

const technologyPartnershipIntegrationFilter = new SearchBuilder()
	.lambda('specificationValues', 'Technology Partner')
	.build();

const lastYearTechnologyPartnershipIntegrationFilter = new SearchBuilder()
	.lt('createDate', lastYearISO)
	.and()
	.lambda('specificationValues', 'Technology Partner')
	.build();

const getAnnualTargetValues = (kpiTarget: string, value: number) => {
	if (kpiTarget.includes('/')) {
		const [current, total] = kpiTarget.split('/');

		return {
			annualTargetCurrent: Number(current),
			annualTargetTotal: Number(total),
		};
	}

	return {
		annualTargetCurrent: Number(value),
		annualTargetTotal: Number(kpiTarget),
	};
};

const queries = [
	HeadlessCommerceAdminCatalog.getProductsDashboardKPI(
		{
			appsAndConnectorSupportingQRelease:
				appsAndConnectorSupportingQReleaseFilter,
			lastYearAppsAndConnectorSupportingQRelease:
				lastYearAppsAndConnectorSupportingQReleaseFilter,
			lastYearLowCodeConfigurationsPublished:
				lastYearLowCodeConfigurationsPublishedFilter,
			lastYearPartnershipIntegration:
				lastYearTechnologyPartnershipIntegrationFilter,
			lowCodeConfigurationsPublished:
				lowCodeConfigurationsPublishedFilter,
			partnershipIntegration: technologyPartnershipIntegrationFilter,
		},
		{
			appsAndConnectorSupportingQRelease: {
				body: ` items { catalogExternalReferenceCode, id, name, thumbnail } `,
				pageSize: -1,
			},
			lastYearAppsAndConnectorSupportingQRelease: {
				body: ` items { catalogExternalReferenceCode, id } `,
				pageSize: -1,
			},
		}
	),
	HeadlessCommerceAdminCatalog.getCatalogs(
		new URLSearchParams({
			fields: 'externalReferenceCode,name',
			pageSize: '-1',
		})
	),
	GraphQL.metrics<{
		externalReferenceCode: string;
		name: string;
		value: string;
	}>(
		{
			group: 'c',
			name: 'reports',
			options: {
				body: `items { externalReferenceCode, name, value }`,
				pageSize: '-1',
				sort: 'dateCreated:desc',
			},
		},
		{
			koroneikiProjects: SearchBuilder.contains(
				'externalReferenceCode',
				'KORONEIKI-PROJECT-'
			),
		}
	),
] as const;

const useKPI = () => {
	const {data: liferayVersionsPicklist} =
		useListTypeDefinition('LIFERAY-VERSIONS');

	const modal = useModalContext();
	const navigate = useNavigate();

	const liferayQuarterlyVersionEntries =
		liferayVersionsPicklist?.listTypeEntries.filter((entry) =>
			entry.externalReferenceCode.includes('Q')
		);

	const liferayQuarterlyVersionsAndConnectors = JSON.stringify({
		'specificationValues|liferayVersion':
			liferayQuarterlyVersionEntries?.map((entry) => entry.name),
	});

	const {
		properties: {
			kpi: anualTargetKPIs,
			lastYearProjectsUsingMarketplaceAppsCount,
		},
	} = useOneContext();

	const {
		kpiConnectorQuartelyRelease,
		kpiLowCodePublishedApps,
		kpiPartnershipIntegration,
		kpiProjectUsingMarketplaceApps,
		kpiQuartelyReleaseApps,
	} = anualTargetKPIs;

	return useSWR('metrics/kpi', async () => {
		const [
			{
				data: {
					metrics: {
						appsAndConnectorSupportingQRelease,
						lastYearAppsAndConnectorSupportingQRelease,
						lastYearLowCodeConfigurationsPublished,
						lastYearPartnershipIntegration,
						lowCodeConfigurationsPublished,
						partnershipIntegration,
					},
				},
			},
			catalogsResponse,
			projectsKPI,
		] = await Promise.all(queries);

		const lastYearSupportingQuartelyReleaseCount = new Set(
			(lastYearAppsAndConnectorSupportingQRelease.items ?? []).map(
				(product) => product.catalogExternalReferenceCode
			)
		).size;

		const lastYearLabel = `${lastYear}`;

		const koroneikiReports =
			projectsKPI?.data?.metrics?.koroneikiProjects?.items ?? [];

		const projectsByKorKey: Record<
			string,
			{accountName: string; orders: unknown[]}
		> = {};

		for (const report of koroneikiReports) {
			const match = report.externalReferenceCode?.match(
				/^KORONEIKI-PROJECT-(.+)$/
			);

			if (!match) {
				continue;
			}

			const korKey = match[1];

			projectsByKorKey[korKey] = safeJSONParse<{
				accountName: string;
				orders: unknown[];
			}>(report.value, {accountName: '', orders: []});
		}

		const projectsUsingMarkeplaceApps = Object.entries(projectsByKorKey);

		const catalogs = Object.groupBy(
			appsAndConnectorSupportingQRelease.items.map((product) => ({
				...product,
				catalogName:
					catalogsResponse.items.find(
						(catalog) =>
							catalog.externalReferenceCode ===
							product.catalogExternalReferenceCode
					)?.name ?? product.externalReferenceCode,
			})),
			({catalogName}) => catalogName
		);

		const supportingQuartelyRelease = {
			...appsAndConnectorSupportingQRelease,
			totalCount: Object.keys(catalogs).length,
		};

		return {
			kpis: [
				{
					...getAnnualTargetValues(
						kpiProjectUsingMarketplaceApps,
						projectsUsingMarkeplaceApps.length
					),
					colors: ['#9CE269', '#D4F3BE'],
					lastYearCount: lastYearProjectsUsingMarketplaceAppsCount
						? Number(lastYearProjectsUsingMarketplaceAppsCount)
						: undefined,
					lastYearLabel,
					onClick: projectsUsingMarkeplaceApps.length
						? () =>
								modal.onOpenModal({
									body: (
										<ProjectsUsingMarketplaceModalBody
											projectsUsingMarkeplaceApps={
												projectsUsingMarkeplaceApps as ComponentProps<
													typeof ProjectsUsingMarketplaceModalBody
												>['projectsUsingMarkeplaceApps']
											}
										/>
									),
									header: i18n.translate(
										'new-projects-using-marketplace-apps'
									),
									size: 'lg',
								})
						: null,
					title: i18n.translate(
						'new-projects-using-marketplace-apps'
					),
				},
				{
					onClick: () =>
						navigate(
							`/admin/publishers?filter={"customFields/AccountType":["${'Technology Partner'}"]}&filterSchema=administratorPublishers`
						),
					...getAnnualTargetValues(
						kpiPartnershipIntegration,
						partnershipIntegration.totalCount
					),
					colors: ['#FFB46E', '#FFE9D4'],
					externalPage: true,
					lastYearCount: lastYearPartnershipIntegration.totalCount,
					lastYearLabel,
					title: i18n.translate(
						'technology-partnership-with-integrations'
					),
				},
				{
					onClick: () =>
						modal.onOpenModal({
							body: (
								<ol>
									{Object.entries(catalogs).map(
										([catalog, products = []], index) => (
											<li key={index}>
												<span className="font-weight-bold">
													{catalog}
												</span>

												<ol>
													{products.map(
														(
															{name},
															productIndex
														) => (
															<li
																key={
																	productIndex
																}
															>
																{name.en_US}
															</li>
														)
													)}
												</ol>
											</li>
										)
									)}
								</ol>
							),
							header: `${i18n.translate('publisher-with-apps-supporting-quarterly-release')} (${supportingQuartelyRelease.totalCount})`,
						}),
					...getAnnualTargetValues(
						kpiQuartelyReleaseApps,
						supportingQuartelyRelease.totalCount
					),
					colors: ['#4B9BFF', '#B1D4FF'],
					lastYearCount: lastYearSupportingQuartelyReleaseCount,
					lastYearLabel,
					title: i18n.translate(
						'publisher-with-apps-supporting-quarterly-release'
					),
				},
				{
					...getAnnualTargetValues(
						kpiConnectorQuartelyRelease,
						appsAndConnectorSupportingQRelease.totalCount
					),
					colors: ['#FF73C3', '#FFE1F0'],
					externalPage: true,
					lastYearCount:
						lastYearAppsAndConnectorSupportingQRelease.totalCount,
					lastYearLabel,
					onClick: () =>
						navigate(
							`/admin/mp-apps?filter=${liferayQuarterlyVersionsAndConnectors}&filterSchema=administratorApps`
						),
					title: i18n.translate(
						'apps-and-connectors-supporting-quarterly-release'
					),
				},
				{
					...getAnnualTargetValues(
						kpiLowCodePublishedApps,
						lowCodeConfigurationsPublished.totalCount
					),
					colors: ['#FFD76E', '#FFF3D4'],
					externalPage: true,
					lastYearCount:
						lastYearLowCodeConfigurationsPublished.totalCount,
					lastYearLabel,
					onClick: () =>
						navigate(
							`/admin/mp-apps?filter={"specificationValues|appType":"${'low-code-configuration'}"}&filterSchema=administratorApps`
						),
					title: i18n.translate('low-code-configurations-published'),
				},
			],
			projectsKPI,
		};
	});
};

export default useKPI;
