/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import AttachmentMessage from '~/pages/TicketAttachments/components/AttachmentMessage/AttachmentMessage';
import routerPath from '~/utils/routerPath';

interface IProps {
	uploadProjectKey?: string;
}

const AttachmentNotFound = ({uploadProjectKey}: IProps) => {
	const pageRoutes = routerPath();

	return (
		<AttachmentMessage
			icon="warning-full"
			subtitle="the-file-may-have-been-deleted"
			title="file-to-download-doesnt-exist-anymore"
		>
			{uploadProjectKey && (
				<a
					className="btn btn-primary"
					href={`${pageRoutes.project(uploadProjectKey)}/attachments`}
				>
					{i18n.translate('return-to-attachments')}
				</a>
			)}
		</AttachmentMessage>
	);
};

export default AttachmentNotFound;
