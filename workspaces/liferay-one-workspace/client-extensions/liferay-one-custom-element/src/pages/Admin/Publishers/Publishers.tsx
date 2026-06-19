/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Label from '@clayui/label';

import ListView from '../../../components/ListView';
import Page from '../../../components/Page';
import SearchBuilder from '../../../core/SearchBuilder';
import i18n from '../../../i18n';
import {formatDate} from '../../../utils/date';

export default function Publishers() {
	return (
		<Page
			pageRendererProps={{className: 'border py-2 rounded-lg'}}
			title={i18n.translate('publishers')}
		>
			<ListView<Account>
				defaultFilters={{
					filter: `${SearchBuilder.eq('type', 'supplier')}`,
				}}
				id="administrator-publishers"
				managementToolbarProps={{
					filterSchema: 'administratorPublishers',
					searchVisible: true,
					visible: true,
				}}
				resource={`/o/headless-admin-user/v1.0/accounts?${new URLSearchParams({sort: 'dateCreated:desc'})}`}
				tableProps={{
					columns: [
						{
							id: 'name',
							name: i18n.translate('name'),
							render: (name, {logoURL}) => (
								<div>
									<img
										className="mr-2 rounded"
										draggable={false}
										height={42}
										src={logoURL}
										width={42}
									/>
									<span className="font-weight-bold">
										{name}
									</span>
								</div>
							),
							sortable: true,
						},
						{
							id: 'id',
							name: i18n.translate('id'),
						},
						{
							id: 'externalReferenceCode',
							name: i18n.translate('external-reference-code'),
						},
						{
							id: 'customFields',
							name: i18n.translate('type'),
							render: (customFields) => {
								const type = customFields?.find(
									({name}) => name === 'AccountType'
								);

								return (
									type?.customValue.data ||
									'Marketplace Publisher'
								);
							},
							sortable: true,
						},
						{
							id: 'dateCreated',
							name: i18n.translate('created-at'),
							render: (createDate) => formatDate(createDate),
							sortable: true,
						},
						{
							id: 'status',
							name: i18n.translate('status'),
							render: (status) => {
								return (
									<Label
										displayType={
											status === 0 ? 'success' : 'warning'
										}
									>
										{status === 0 ? 'Approved' : 'Pending'}
									</Label>
								);
							},
						},
					],
				}}
			/>
		</Page>
	);
}
