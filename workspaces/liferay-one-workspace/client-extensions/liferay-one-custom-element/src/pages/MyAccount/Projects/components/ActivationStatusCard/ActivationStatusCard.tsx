/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import {Word, translate} from '~/i18n';

import './ActivationStatusCard.css';

type StatusConfig = {
	icon: string;
	subtitle: Word;
	title: Word;
	type: string;
};

const CONFIG_BY_PRODUCT_NAME: {[name: string]: StatusConfig} = {
	'Analytics Cloud': {
		icon: 'analytics',
		subtitle:
			'almost-there-setup-analytics-cloud-by-finishing-the-activation-form',
		title: 'analytics-cloud-activation',
		type: 'SaaS',
	},
	'PaaS': {
		icon: 'cloud',
		subtitle:
			'almost-there-setup-liferay-paas-by-finishing-the-activation-form',
		title: 'liferay-paas-activation',
		type: 'PaaS',
	},
	'SaaS': {
		icon: 'cloud',
		subtitle:
			'almost-there-setup-liferay-saas-by-finishing-the-activation-form',
		title: 'liferay-saas-activation',
		type: 'SaaS',
	},
};

const STATUS_LABEL: Record<
	string,
	{displayType: 'secondary' | 'success' | 'warning'; label: Word}
> = {
	active: {displayType: 'success', label: 'active'},
	deactivated: {displayType: 'secondary', label: 'deactivated'},
	expired: {displayType: 'warning', label: 'expired'},
};

type ActivationStatusCardProps = {
	productName: string;
};

export default function ActivationStatusCard({
	productName,
}: ActivationStatusCardProps) {
	const {environments} = useProjectEnvironments();

	const config =
		CONFIG_BY_PRODUCT_NAME[productName] ??
		CONFIG_BY_PRODUCT_NAME['Analytics Cloud'];

	const environment =
		environments.find((current) => current.type === config.type) ??
		environments[0];

	const status = STATUS_LABEL[environment?.status ?? ''] ?? {
		displayType: 'secondary' as const,
		label: 'not-activated' as Word,
	};

	return (
		<div className="mt-3">
			<h2>{translate(config.title)}</h2>

			<p className="text-neutral-7">{translate(config.subtitle)}</p>

			<div className="activation-status-card">
				<span className="activation-status-card-icon">
					<ClayIcon symbol={config.icon} />
				</span>

				<div className="flex-grow-1">
					<span className="fw-bold">{productName}</span>

					<p className="list-card-subtext m-0">
						{environment
							? `${environment.type}${
									environment.region
										? ` • ${environment.region}`
										: ''
								}`
							: translate('no-environment-yet')}
					</p>
				</div>

				<ClayLabel displayType={status.displayType}>
					{translate(status.label)}
				</ClayLabel>
			</div>
		</div>
	);
}
