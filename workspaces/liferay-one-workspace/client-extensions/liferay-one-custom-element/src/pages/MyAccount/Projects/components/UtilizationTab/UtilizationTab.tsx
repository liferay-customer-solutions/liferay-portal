/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProjectUsage} from '~/hooks/useProjectUsage';
import {buildUtilizationSections} from '~/pages/MyAccount/Projects/utils/buildUtilizationSections';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';

import SectionedDetailsCard from '../SectionedDetailsCard/SectionedDetailsCard';
import UtilizationCard from '../UtilizationCard/UtilizationCard';
import LegacyBillingBanner from './LegacyBillingBanner';
import LegacyDashboardPreview from './LegacyDashboardPreview';
import UsageDashboard from './UsageDashboard';

import type {UtilizationProfile} from '~/pages/MyAccount/Projects/utils/resolveUtilizationProfile';

const DASHBOARD_PROFILES: UtilizationProfile[] = [
	'experience-dashboard',
	'saas-plan-dashboard',
];

type UtilizationTabProps = {
	productExternalReferenceCode: string;
	profile?: UtilizationProfile;
	projectExternalReferenceCode: string;
};

export default function UtilizationTab({
	productExternalReferenceCode,
	profile,
	projectExternalReferenceCode,
}: UtilizationTabProps) {
	const showDashboard =
		profile !== undefined &&
		DASHBOARD_PROFILES.includes(profile) &&
		!isUnassignedProject(projectExternalReferenceCode);

	const {usage} = useProjectUsage(showDashboard || profile === 'legacy');

	if (profile === 'legacy') {
		return (
			<>
				<LegacyBillingBanner />

				<LegacyDashboardPreview />
			</>
		);
	}

	if (showDashboard) {
		return (
			<UsageDashboard
				productExternalReferenceCode={productExternalReferenceCode}
				profile={profile}
				projectExternalReferenceCode={projectExternalReferenceCode}
			/>
		);
	}

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
