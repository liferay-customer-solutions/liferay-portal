/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {isUnassignedProject} from '~/pages/MyAccount/Projects/utils/isUnassignedProject';

import LDPUsageDashboard from './LDPUsageDashboard';
import LegacyBillingBanner from './LegacyBillingBanner';
import LegacyDashboardPreview from './LegacyDashboardPreview';
import UsageDashboard from './UsageDashboard';
import UsageEventsCard from './UsageEventsCard';

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
	if (profile === 'legacy') {
		return (
			<>
				<LegacyBillingBanner />

				<LegacyDashboardPreview />
			</>
		);
	}

	if (
		profile !== undefined &&
		DASHBOARD_PROFILES.includes(profile) &&
		!isUnassignedProject(projectExternalReferenceCode)
	) {
		return (
			<UsageDashboard
				productExternalReferenceCode={productExternalReferenceCode}
				profile={profile}
				projectExternalReferenceCode={projectExternalReferenceCode}
			/>
		);
	}

	if (
		profile === 'usage-metrics' &&
		!isUnassignedProject(projectExternalReferenceCode)
	) {
		return (
			<LDPUsageDashboard
				productExternalReferenceCode={productExternalReferenceCode}
				projectExternalReferenceCode={projectExternalReferenceCode}
			/>
		);
	}

	return <UsageEventsCard />;
}
