/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';
import {useProjectUsageDashboard} from '~/pages/MyAccount/Projects/hooks/useProjectUsageDashboard';
import {LDP_SUMMARY_METRICS} from '~/pages/MyAccount/Projects/utils/usageDashboardMetricsConstants';

import UtilizationCard from '../UtilizationCard/UtilizationCard';
import LDPSummaryCard from './LDPSummaryCard';
import UsageUnavailableBanner from './UsageUnavailableBanner';
import UsageUnavailableCard from './UsageUnavailableCard';

import './LDPSummaryCard.css';

type LDPUsageDashboardProps = {
	productExternalReferenceCode: string;
	projectExternalReferenceCode: string;
};

export default function LDPUsageDashboard({
	productExternalReferenceCode,
	projectExternalReferenceCode,
}: LDPUsageDashboardProps) {
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

	const metrics = usageDashboard?.metrics;

	if (
		!metrics ||
		!LDP_SUMMARY_METRICS.some(
			(metricConfig) => metrics[metricConfig.metric]
		)
	) {
		return <UtilizationCard />;
	}

	return (
		<>
			{usageDashboard?.usageDataAvailable === false && (
				<div className="mt-3">
					<UsageUnavailableBanner />
				</div>
			)}

			<div className="ldp-summary-grid mt-3">
				{LDP_SUMMARY_METRICS.map((metricConfig) => (
					<LDPSummaryCard
						key={metricConfig.metric}
						label={metricConfig.label}
						metric={metrics[metricConfig.metric]}
						tooltip={metricConfig.tooltip}
					/>
				))}
			</div>
		</>
	);
}
