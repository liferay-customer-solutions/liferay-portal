/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLink from '@clayui/link';
import {useEffect} from 'react';
import i18n from '~/i18n';
import AttachmentMessage from '~/pages/TicketAttachments/components/AttachmentMessage/AttachmentMessage';

interface IProps {
	downloadUrl: string;
}

const TicketAttachmentsDownloader = ({downloadUrl}: IProps) => {
	useEffect(() => {
		const a = document.createElement('a');

		a.href = downloadUrl;

		document.body.appendChild(a);

		a.click();

		a.remove();
	}, [downloadUrl]);

	return (
		<AttachmentMessage
			icon="download"
			subtitle="the-file-is-downloading-automatically-if-you-encounter-any-issues-click-the-download-button-below-to-start-the-download-manually"
			title="download-in-progress"
		>
			<ClayLink className="btn btn-primary" href={downloadUrl}>
				{i18n.translate('download')}
			</ClayLink>
		</AttachmentMessage>
	);
};

export default TicketAttachmentsDownloader;
