/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Label from '@clayui/label';

import {orderTypeLabel} from '../../../../enums/Order';
import i18n from '../../../../i18n';

type ProjectOrder = {
	creatorEmailAddress: string;
	id: number;
	orderTypeExternalReferenceCode: string;
	projects: {
		key: string;
		name: string;
	}[];
};

type ProjectsUsingMarketplaceModalBodyProps = {
	projectsUsingMarkeplaceApps: [
		string,
		{
			accountName: string;
			orders: ProjectOrder[];
		},
	][];
};

type ProjectUsingMarketplaceProps = {
	index: number;
	order: ProjectOrder;
};

function ProjectUsingMarketplace({index, order}: ProjectUsingMarketplaceProps) {
	const exactMatch =
		order.projects.length === 1 ||
		order.orderTypeExternalReferenceCode.startsWith('KOR-');

	return (
		<details className="border-0 list-group-item py-1" key={index}>
			<summary>
				<span>
					<Label displayType={exactMatch ? 'success' : 'warning'}>
						{exactMatch
							? i18n.translate('exact-match')
							: i18n.translate('multiple-projects')}
					</Label>{' '}
					Order #{order.id}
				</span>{' '}
				–{' '}
				<span className="text-muted">
					{
						orderTypeLabel[
							order.orderTypeExternalReferenceCode as keyof typeof orderTypeLabel
						]
					}
				</span>
			</summary>

			<p>
				{i18n.translate('created-by')}: {order.creatorEmailAddress}
			</p>

			{!exactMatch && (
				<p>
					{i18n.translate('projects')}:{' '}
					{order.projects.map((customerProject, index) => (
						<Label key={index}>{customerProject.name}</Label>
					))}
				</p>
			)}
		</details>
	);
}

export default function ProjectsUsingMarketplaceModalBody({
	projectsUsingMarkeplaceApps,
}: ProjectsUsingMarketplaceModalBodyProps) {
	return (
		<ul className="list-group list-group-flush">
			{projectsUsingMarkeplaceApps.map(([key, project], index) => (
				<li className="list-group-item" key={index}>
					<div className="mb-1">
						<strong className="mr-1 text-dark">
							[{index + 1}] {key}
						</strong>

						<span className="text-dark">{project.accountName}</span>
					</div>

					{project.orders?.map((order, index) => (
						<ProjectUsingMarketplace
							index={index}
							key={index}
							order={order}
						/>
					))}
				</li>
			))}
		</ul>
	);
}
