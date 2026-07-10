/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProjectUsage} from '~/hooks/useProjectUsage';
import {buildUtilizationSections} from '~/pages/MyAccount/Projects/utils/buildUtilizationSections';

import SectionedDetailsCard from '../SectionedDetailsCard/SectionedDetailsCard';
import UtilizationCard from '../UtilizationCard/UtilizationCard';

export default function UtilizationTab() {
	const {usage} = useProjectUsage();

	if (!usage.length) {
		return <UtilizationCard />;
	}

	return (
		<SectionedDetailsCard
			icon="analytics"
			sections={buildUtilizationSections(usage)}
			title="usage"
		/>
	);
}
