/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useParams} from 'react-router-dom';

import i18n, {Word} from '../../../i18n';
import DetailHeader from './DetailHeader';
import DetailsCard from './DetailsCard';
import ProjectDetailTabs, {DetailTab} from './ProjectDetailTabs';
import {getApplication} from './applications';

export default function ApplicationDetails() {
	const {applicationId} = useParams();

	const application = getApplication(applicationId ?? '');

	if (!application) {
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
				{label: i18n.translate('order-id'), value: application.orderId},
				{
					label: i18n.translate('order-date'),
					value: application.orderDate,
				},
				{
					label: i18n.translate('purchased-by'),
					value: application.providedBy,
				},
				{
					label: i18n.translate('purchase-number'),
					value: application.purchaseNumber,
				},
				{
					label: i18n.translate('project-type'),
					value: i18n.translate(application.projectType as Word),
				},
				{
					label: i18n.translate('customer-account'),
					value: application.customerAccount,
				},
			]}
		/>
	);

	const tabs: DetailTab[] = [
		{content: detailsContent, key: 'details', label: 'details'},
		{content: null, key: 'activation', label: 'activation'},
		{content: null, key: 'download', label: 'download'},
		{
			content: null,
			key: 'help-and-support',
			label: 'help-and-support',
		},
	];

	return (
		<ProjectDetailTabs
			header={
				<DetailHeader
					logoColor={application.logoColor}
					name={application.name}
					publisher={application.publisher}
					status={application.status}
				/>
			}
			tabs={tabs}
		/>
	);
}
