/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useState} from 'react';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {useUserProjects} from '~/pages/MyAccount/Projects/projects';
import AttachmentAlreadyExists from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/AttachmentAlreadyExists';
import AttachmentNotFound from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/AttachmentNotFound';
import ForbiddenAccessUpload from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/ForbiddenAccessUpload';
import InvalidTicketNumber from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/InvalidTicketNumber';
import TicketIsClosed from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/TicketIsClosed';
import UnexpectedError from '~/pages/TicketAttachments/components/TicketAttachmentsMessages/UnexpectedError';
import useCheckAttachmentAccess from '~/pages/TicketAttachments/hooks/useCheckAttachmentAccess';
import {IUpload} from '~/utils/types';

import TicketAttachmentsUploader from './TicketAttachmentsUploader';

const renderErrorComponent = (
	errorCode: string | null,
	uploadStateData: IUpload | null
) => {
	switch (errorCode) {
		case 'FORBIDDEN_ACCESS':
			return <ForbiddenAccessUpload />;
		case 'INVALID_TICKET_NUMBER':
			return <InvalidTicketNumber />;
		case 'JIRA_ORGANIZATION_ERROR': {
			if (!uploadStateData?.uploadProjectKey) {
				return <UnexpectedError uploadErrorMessage="try-again-later" />;
			}

			return (
				<AttachmentNotFound
					uploadProjectKey={uploadStateData.uploadProjectKey}
				/>
			);
		}
		case 'TICKET_IS_CLOSED':
			return <TicketIsClosed />;
		default:
			return (
				<UnexpectedError
					uploadErrorMessage={
						uploadStateData?.errorMessage ?? 'try-again-later'
					}
				/>
			);
	}
};

const TicketAttachmentsUploaderOutlet = () => {
	const {errorCode, hasAccess, loading} = useCheckAttachmentAccess();
	const {loading: projectsLoading, projects} = useUserProjects();
	const [uploadStateData, setUploadStateData] = useState<IUpload | null>(
		null
	);

	if (loading || projectsLoading) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!projects.length) {
		return <RestrictedFeatureMessage />;
	}

	if (uploadStateData?.errorCode === 'ATTACHMENT_ALREADY_EXISTS') {
		return <AttachmentAlreadyExists />;
	}

	if (hasAccess) {
		return (
			<TicketAttachmentsUploader
				setUploadStateData={setUploadStateData}
				uploadStateData={uploadStateData}
			/>
		);
	}

	return renderErrorComponent(errorCode, uploadStateData);
};

export default TicketAttachmentsUploaderOutlet;
