/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import AttachmentMessage from '~/pages/TicketAttachments/components/AttachmentMessage/AttachmentMessage';
import routerPath from '~/utils/routerPath';

interface IProps {
	ticketURL: string;
	uploadProjectKey: string;
}

const ServerUnavailable = ({ticketURL, uploadProjectKey}: IProps) => {
	const pageRoutes = routerPath();

	return (
		<AttachmentMessage
			icon="warning-full"
			subtitle="try-again-later"
			title="unable-to-connect-to-file-server"
		>
			{uploadProjectKey && (
				<a
					className="btn btn-secondary mr-2 uploader-secondary-button"
					href={`${pageRoutes.project(uploadProjectKey)}/attachments`}
				>
					{i18n.translate('return-to-attachments')}
				</a>
			)}

			{ticketURL && (
				<a
					className="btn btn-primary uploader-primary-button"
					href={`${ticketURL}`}
				>
					{i18n.translate('return-to-ticket')}
				</a>
			)}
		</AttachmentMessage>
	);
};

export default ServerUnavailable;
