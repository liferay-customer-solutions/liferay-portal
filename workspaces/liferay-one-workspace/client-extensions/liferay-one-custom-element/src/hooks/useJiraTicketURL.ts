/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProperties} from '~/context/PropertiesContext';

export default function useJiraTicketURL(ticketId: string) {
	const {jiraFLSPortalURL, jiraFLSProject, jiraHCPortalURL} = useProperties();

	if (jiraFLSProject && ticketId.startsWith(jiraFLSProject)) {
		return `${jiraFLSPortalURL ?? ''}/${ticketId}`;
	}

	return `${jiraHCPortalURL ?? ''}/${ticketId}`;
}
