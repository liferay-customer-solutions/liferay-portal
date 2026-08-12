/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n from '~/i18n';
import {useProjectUsageDashboard} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';
import {
	EXPERIENCE_RESOURCE_METRICS,
	SAAS_RESOURCE_METRICS,
	SAAS_SITE_AND_USER_METRICS,
} from '~/pages/MyAccount/Projects/utils/usageDashboardMetricsConstants';
import {hasOverageUsage} from '~/pages/MyAccount/Projects/utils/usageMetricDisplayUtils';
import {Liferay} from '~/services/liferay/liferay';

import UtilizationCard from '../UtilizationCard/UtilizationCard';
import OverageBanner from './OverageBanner';
import UsageDonut from './UsageDonut';
import UsageMetricCard from './UsageMetricCard';
import UsageProgressBar from './UsageProgressBar';
import UsageSection from './UsageSection';
import UsageUnavailableCard from './UsageUnavailableCard';

import './UsageDashboard.css';

import type {UsageDashboard as UsageDashboardType} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';
import type {UtilizationProfile} from '~/pages/MyAccount/Projects/utils/resolveUtilizationProfile';
import type {UsageMetricConfig} from '~/pages/MyAccount/Projects/utils/usageDashboardMetricsConstants';

type UsageDashboardProps = {
	productExternalReferenceCode: string;
	profile: UtilizationProfile;
	projectExternalReferenceCode: string;
};

function formatBillingPeriod(): string {
	const dateTimeFormat = new Intl.DateTimeFormat(
		Liferay.ThemeDisplay.getBCP47LanguageId(),
		{month: 'long', timeZone: 'UTC', year: 'numeric'}
	);

	return dateTimeFormat.format(new Date());
}

function renderDonuts(
	metricConfigs: UsageMetricConfig[],
	usageDashboard: UsageDashboardType
) {
	return metricConfigs.map((metricConfig) => (
		<UsageMetricCard key={metricConfig.metric}>
			<UsageDonut
				label={metricConfig.label}
				metric={usageDashboard.metrics[metricConfig.metric]}
				totalLabel={metricConfig.totalLabel}
			/>
		</UsageMetricCard>
	));
}

export default function UsageDashboard({
	productExternalReferenceCode,
	profile,
	projectExternalReferenceCode,
}: UsageDashboardProps) {
	const {error, isLoading, usageDashboard} = useProjectUsageDashboard(
		productExternalReferenceCode,
		projectExternalReferenceCode
	);

	if (isLoading) {
		return (
			<p className="mt-3 text-neutral-7">{i18n.translate('loading')}</p>
		);
	}

	if (error) {
		return <UsageUnavailableCard />;
	}

	if (
		!usageDashboard?.metrics ||
		!Object.keys(usageDashboard.metrics).length
	) {
		return <UtilizationCard />;
	}

	const isExperience = profile === 'experience-dashboard';

	return (
		<DetailedCard
			cardIconAltText={i18n.translate('utilization')}
			cardTitle={i18n.translate('project-usage-metrics')}
			className="mt-3"
			clayIcon="analytics"
		>
			{hasOverageUsage(usageDashboard.metrics) && <OverageBanner />}

			<p className="text-neutral-7 text-small">
				{`${i18n.translate('billing-period')}: ${formatBillingPeriod()}`}
			</p>

			{!isExperience && (
				<UsageSection title="sites-and-users">
					{SAAS_SITE_AND_USER_METRICS.map((metricConfig) => (
						<UsageMetricCard key={metricConfig.metric}>
							<UsageProgressBar
								label={metricConfig.label}
								metric={
									usageDashboard.metrics[metricConfig.metric]
								}
							/>
						</UsageMetricCard>
					))}
				</UsageSection>
			)}

			<UsageSection title="resource-usage">
				{renderDonuts(
					isExperience
						? EXPERIENCE_RESOURCE_METRICS
						: SAAS_RESOURCE_METRICS,
					usageDashboard
				)}
			</UsageSection>
		</DetailedCard>
	);
}
