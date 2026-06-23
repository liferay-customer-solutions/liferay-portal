/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {ProductEnvironmentInfo} from '~/hooks/useProjectOrders';
import i18n, {Word} from '~/i18n';

import DetailsCard from '../DetailsCard/DetailsCard';

type EnvironmentCardProps = {
	environment: ProductEnvironmentInfo;
};

const ENVIRONMENT_ROWS: {key: keyof ProductEnvironmentInfo; label: Word}[] = [
	{key: 'projectName', label: 'project-name'},
	{key: 'cloudProjectName', label: 'cloud-project'},
];

export default function EnvironmentCard({environment}: EnvironmentCardProps) {
	const rows = ENVIRONMENT_ROWS.filter((row) => environment[row.key]).map(
		(row) => ({
			label: i18n.translate(row.label),
			value: environment[row.key],
		})
	);

	if (!rows.length) {
		return (
			<DetailedCard
				cardIconAltText={i18n.translate('workspace-info')}
				cardTitle={i18n.translate('workspace-info')}
				className="mt-3"
				clayIcon="cloud"
			>
				<p className="mt-3 text-neutral-7">
					{i18n.translate('no-environment-information-yet')}
				</p>
			</DetailedCard>
		);
	}

	return <DetailsCard icon="cloud" rows={rows} title="workspace-info" />;
}
