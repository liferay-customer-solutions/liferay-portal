/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import {buildEnvironmentSections} from '~/pages/MyAccount/Projects/utils/buildEnvironmentSections';
import {filterEnvironmentsByContract} from '~/pages/MyAccount/Projects/utils/filterEnvironmentsByContract';

import AIHubEnvironment from '../AIHubEnvironment/AIHubEnvironment';
import DSREnvironment from '../DSREnvironment/DSREnvironment';
import EnvironmentCard from '../EnvironmentCard/EnvironmentCard';
import SectionedDetailsCard from '../SectionedDetailsCard/SectionedDetailsCard';

import type {ProductEnvironmentInfo} from '~/hooks/useProjectOrders';
import type {EnvironmentProfile} from '~/pages/MyAccount/Projects/utils/resolveEnvironmentProfile';

type EnvironmentTabProps = {
	contractId?: number;
	environment: ProductEnvironmentInfo;
	profile?: EnvironmentProfile;
};

const ENVIRONMENT_TYPE_BY_PROFILE: Record<EnvironmentProfile, string> = {
	'ac-token': 'DSR',
	'ai-hub': 'AI Hub',
	'analytics-cloud': 'SaaS',
	'none': '',
	'paas': 'PaaS',
	'saas': 'SaaS',
	'workspace': 'LDP',
};

export default function EnvironmentTab({
	contractId,
	environment,
	profile,
}: EnvironmentTabProps) {
	const {environments} = useProjectEnvironments();

	const expectedType = profile ? ENVIRONMENT_TYPE_BY_PROFILE[profile] : '';

	const matchingEnvironments = filterEnvironmentsByContract(
		contractId,
		environments.filter((item) => item.type === expectedType)
	);

	const [environmentEntry] = matchingEnvironments;

	if (!profile || !environmentEntry) {
		return <EnvironmentCard environment={environment} />;
	}

	if (profile === 'ai-hub') {
		return <AIHubEnvironment environment={environmentEntry} />;
	}

	if (profile === 'ac-token') {
		return <DSREnvironment environment={environmentEntry} />;
	}

	return (
		<SectionedDetailsCard
			icon="cloud"
			sections={buildEnvironmentSections(matchingEnvironments, profile)}
			title="workspace-info"
		/>
	);
}
