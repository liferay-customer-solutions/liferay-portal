/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import {buildEnvironmentSections} from '~/pages/MyAccount/Projects/utils/buildEnvironmentSections';

import EnvironmentCard from '../EnvironmentCard/EnvironmentCard';
import SectionedDetailsCard from '../SectionedDetailsCard/SectionedDetailsCard';

import type {ProductEnvironmentInfo} from '~/hooks/useProjectOrders';

type EnvironmentTabProps = {
	environment: ProductEnvironmentInfo;
};

export default function EnvironmentTab({environment}: EnvironmentTabProps) {
	const {environments} = useProjectEnvironments();

	if (!environments.length) {
		return <EnvironmentCard environment={environment} />;
	}

	return (
		<SectionedDetailsCard
			icon="cloud"
			sections={buildEnvironmentSections(environments)}
			title="workspace-info"
		/>
	);
}
