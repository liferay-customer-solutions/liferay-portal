/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '../../../../../components/DetailedCard/DetailedCard';
import QATable from '../../../../../components/QATable';
import i18n from '../../../../../i18n';

type WorkspaceInfoCardProps = {
	analyticsProject?: any;
};

const WorkspaceInfoCard: React.FC<WorkspaceInfoCardProps> = ({
	analyticsProject,
}) => {
	const allowedEmailDomains = analyticsProject?.allowedEmailDomains || [];

	const incidentReportEmailAddresses =
		analyticsProject?.incidentReportEmailAddresses || [];

	return (
		<DetailedCard
			cardIconAltText="Summary Icon"
			cardTitle={i18n.translate('workspace-info')}
			clayIcon="polls"
		>
			<QATable
				items={[
					{
						title: i18n.translate('workspace-name'),
						value: analyticsProject?.corpProjectName,
					},
					{
						title: i18n.translate('workspace-owner-email'),
						value: analyticsProject?.ownerEmailAddress,
					},
					{
						title: i18n.translate('data-center-location'),
						value: analyticsProject?.serverLocation,
					},
					{
						title: i18n.translate('timezone'),
						value: analyticsProject?.timeZone?.displayTimeZone,
					},
					{
						title: i18n.translate('workspace-friendly-url'),
						value: analyticsProject?.friendlyURL,
					},
					{
						title: i18n.translate('allowed-email-domains'),
						value: allowedEmailDomains?.map(
							(emailAddress: string) => (
								<div key={emailAddress}>{emailAddress}</div>
							)
						),
					},
					{
						title: i18n.translate('incident-report-contacts'),
						value: incidentReportEmailAddresses?.map(
							(emailAddress: string) => (
								<div key={emailAddress}>{emailAddress}</div>
							)
						),
					},
				]}
			/>
		</DetailedCard>
	);
};

export default WorkspaceInfoCard;
