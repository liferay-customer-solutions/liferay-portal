/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import ClayLink from '@clayui/link';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useNavigate} from 'react-router-dom';
import Table, {
	IRow,
} from '~/components/BusinessEventsTable/BusinessEventsTable';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {useProperties} from '~/context/PropertiesContext';
import {useFetch} from '~/hooks/useFetch';
import {translate} from '~/i18n';
import {useUserProjects} from '~/pages/MyAccount/Projects/projects';
import formatFileSize from '~/pages/TicketAttachments/utils/formatFileSize';

import type {APIResponse} from '~/types/api';

interface ITicketAttachment {
	dateCreated: string;
	externalReferenceCode: string;
	fileName: string;
	fileSize: string;
	id: number;
	jiraIssueKey: string;
}

const columns = [
	{
		columnKey: 'fileName',
		label: translate('file-name'),
	},
	{
		columnKey: 'jiraIssueKey',
		label: translate('ticket'),
	},
	{
		columnKey: 'fileSize',
		label: translate('size'),
	},
	{
		columnKey: 'dateCreated',
		label: translate('date-added'),
	},
	{
		columnKey: 'actions',
		label: '',
	},
];

const TicketAttachmentsList = () => {
	const navigate = useNavigate();

	const {jiraFLSPortalURL, jiraFLSProject, jiraHCPortalURL} = useProperties();

	const {loading: projectsLoading, projects} = useUserProjects();

	const {data, isLoading: loading} = useFetch<APIResponse<ITicketAttachment>>(
		'/o/c/ticketattachments',
		{
			params: {
				pageSize: 200,
				sort: 'dateCreated:desc',
			},
		}
	);

	const getTicketURL = (jiraIssueKey: string) => {
		if (jiraFLSProject && jiraIssueKey.startsWith(jiraFLSProject)) {
			return `${jiraFLSPortalURL ?? ''}/${jiraIssueKey}`;
		}

		return `${jiraHCPortalURL ?? ''}/${jiraIssueKey}`;
	};

	const header = (
		<div className="align-items-start d-flex justify-content-between">
			<div>
				<h1 className="font-weight-bold text-neutral-10">
					{translate('ticket-attachments')}
				</h1>

				<h6 className="font-weight-normal text-neutral-7">
					{translate(
						'upload-and-download-large-files-associated-with-your-support-tickets'
					)}
				</h6>
			</div>

			<Button displayType="primary" onClick={() => navigate('/new')}>
				{translate('new-attachment')}
			</Button>
		</div>
	);

	if (loading || projectsLoading) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!projects.length) {
		return (
			<div className="py-4">
				{header}

				<RestrictedFeatureMessage />
			</div>
		);
	}

	const attachments = data?.items ?? [];

	const rows = attachments.map((attachment) => ({
		actions: (
			<div className="d-flex justify-content-end">
				<ClayLink
					href={`#/id/${attachment.id}`}
					title={translate('download')}
				>
					{translate('download')}
				</ClayLink>
			</div>
		),
		dateCreated: (
			<div className="text-neutral-10">
				{new Date(attachment.dateCreated).toLocaleDateString()}
			</div>
		),
		fileName: (
			<div className="font-weight-semi-bold text-neutral-10">
				{attachment.fileName}
			</div>
		),
		fileSize: (
			<div className="text-neutral-10">
				{formatFileSize(attachment.fileSize)}
			</div>
		),
		jiraIssueKey: attachment.jiraIssueKey ? (
			<ClayLink
				href={getTicketURL(attachment.jiraIssueKey)}
				target="_blank"
			>
				{attachment.jiraIssueKey}
			</ClayLink>
		) : (
			''
		),
	}));

	return (
		<div className="py-4">
			{header}

			<div className="mt-3">
				{attachments.length ? (
					<Table columns={columns} rows={rows as unknown as IRow[]} />
				) : (
					<div className="p-3">
						{translate('no-ticket-attachments-were-found')}
					</div>
				)}
			</div>
		</div>
	);
};

export default TicketAttachmentsList;
