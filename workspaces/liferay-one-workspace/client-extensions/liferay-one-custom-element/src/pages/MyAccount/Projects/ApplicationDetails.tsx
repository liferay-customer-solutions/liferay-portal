/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ProjectDetailTabs, {DetailTab} from './ProjectDetailTabs';

const tabs: DetailTab[] = [
	{content: null, key: 'details', label: 'details'},
	{content: null, key: 'activation', label: 'activation'},
	{content: null, key: 'help-and-support', label: 'help-and-support'},
];

export default function ApplicationDetails() {
	return <ProjectDetailTabs tabs={tabs} />;
}
