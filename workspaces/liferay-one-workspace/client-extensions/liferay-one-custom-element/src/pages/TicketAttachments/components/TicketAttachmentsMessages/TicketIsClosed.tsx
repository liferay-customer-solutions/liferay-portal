/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import AttachmentMessage from '~/pages/TicketAttachments/components/AttachmentMessage/AttachmentMessage';

const CREATE_TICKET_URL =
	'https://liferay.atlassian.net/servicedesk/customer/portals';

const TicketIsClosed = () => {
	return (
		<AttachmentMessage
			icon="warning-full"
			subtitle={i18n.translate(
				'no-further-edits-can-be-made-when-tickets-are-closed-please-open-a-new-support-ticket-if-assistance-is-needed'
			)}
			title={i18n.translate('this-ticket-has-been-closed')}
		>
			<a className="btn btn-primary" href={CREATE_TICKET_URL}>
				{i18n.translate('create-new-ticket')}
			</a>
		</AttachmentMessage>
	);
};

export default TicketIsClosed;
