/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '../../../i18n';
import DetailsCard from './DetailsCard';
import {Application} from './applications';
import {getHelpSupportLinks} from './tabData';

type HelpSupportCardProps = {
	application: Application;
};

export default function HelpSupportCard({application}: HelpSupportCardProps) {
	const rows = getHelpSupportLinks(application).map((link) => ({
		label: i18n.translate(link.label),
		value: (
			<a href={link.href} rel="noopener noreferrer" target="_blank">
				{link.value}
			</a>
		),
	}));

	return (
		<DetailsCard
			icon="question-circle"
			rows={rows}
			title="help-and-support"
		/>
	);
}
