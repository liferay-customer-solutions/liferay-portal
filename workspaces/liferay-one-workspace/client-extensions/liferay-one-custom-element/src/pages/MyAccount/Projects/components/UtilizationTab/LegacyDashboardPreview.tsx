/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n from '~/i18n';
import {
	SAAS_RESOURCE_METRICS,
	SAAS_SITE_AND_USER_METRICS,
} from '~/pages/MyAccount/Projects/utils/usageDashboardMetricsConstants';

import UsageDonut from './UsageDonut';
import UsageMetricCard from './UsageMetricCard';
import UsageProgressBar from './UsageProgressBar';
import UsageSection from './UsageSection';

export default function LegacyDashboardPreview() {
	return (
		<DetailedCard
			cardIconAltText={i18n.translate('utilization')}
			cardTitle={i18n.translate('project-usage-metrics')}
			className="mt-3"
			clayIcon="analytics"
		>
			<div aria-hidden="true" className="usage-dashboard-legacy-preview">
				<UsageSection title="sites-and-users">
					{SAAS_SITE_AND_USER_METRICS.map((metricConfig) => (
						<UsageMetricCard key={metricConfig.metric}>
							<UsageProgressBar label={metricConfig.label} />
						</UsageMetricCard>
					))}
				</UsageSection>

				<UsageSection title="resource-usage">
					{SAAS_RESOURCE_METRICS.map((metricConfig) => (
						<UsageMetricCard key={metricConfig.metric}>
							<UsageDonut
								label={metricConfig.label}
								totalLabel={metricConfig.totalLabel}
							/>
						</UsageMetricCard>
					))}
				</UsageSection>
			</div>
		</DetailedCard>
	);
}
