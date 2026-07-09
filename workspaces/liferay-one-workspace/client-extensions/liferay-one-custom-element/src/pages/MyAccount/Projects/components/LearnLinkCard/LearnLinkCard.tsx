/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import DetailsCard from '../DetailsCard/DetailsCard';

type LearnLinkCardProps = {
	url: string;
};

export default function LearnLinkCard({url}: LearnLinkCardProps) {
	return (
		<DetailsCard
			icon="question-circle"
			rows={[
				{
					label: i18n.translate('liferay-learn'),
					value: (
						<a href={url} rel="noopener noreferrer" target="_blank">
							{url}
						</a>
					),
				},
			]}
			title="help-and-support"
		/>
	);
}
