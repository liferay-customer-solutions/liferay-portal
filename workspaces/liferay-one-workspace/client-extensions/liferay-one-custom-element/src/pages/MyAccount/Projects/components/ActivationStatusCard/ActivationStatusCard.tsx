/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import {Word, translate} from '~/i18n';
import {
	ACTIVATION_STATUS_MOCK,
	ActivationStatusState,
} from '~/pages/MyAccount/Projects/utils/activationMockDataConstants';

import './ActivationStatusCard.css';

type StatusConfig = {
	icon: string;
	subtitle: Word;
	title: Word;
};

const CONFIG_BY_PRODUCT_NAME: {[name: string]: StatusConfig} = {
	'Analytics Cloud': {
		icon: 'analytics',
		subtitle: 'almost-there-setup-analytics-cloud-by-finishing-the-activation-form',
		title: 'analytics-cloud-activation',
	},
	'PaaS': {
		icon: 'cloud',
		subtitle: 'almost-there-setup-liferay-paas-by-finishing-the-activation-form',
		title: 'liferay-paas-activation',
	},
	'SaaS': {
		icon: 'cloud',
		subtitle: 'almost-there-setup-liferay-saas-by-finishing-the-activation-form',
		title: 'liferay-saas-activation',
	},
};

const STATUS_LABEL: Record<
	ActivationStatusState,
	{displayType: 'secondary' | 'success' | 'warning'; label: Word}
> = {
	'active': {displayType: 'success', label: 'active'},
	'in-progress': {displayType: 'warning', label: 'in-progress'},
	'not-activated': {displayType: 'secondary', label: 'not-activated'},
};

type ActivationStatusCardProps = {
	productName: string;
};

export default function ActivationStatusCard({
	productName,
}: ActivationStatusCardProps) {
	const config =
		CONFIG_BY_PRODUCT_NAME[productName] ??
		CONFIG_BY_PRODUCT_NAME['Analytics Cloud'];

	const status = STATUS_LABEL[ACTIVATION_STATUS_MOCK.status];

	return (
		<div className="mt-3">
			<h2>{translate(config.title)}</h2>

			<p className="text-neutral-7">{translate(config.subtitle)}</p>

			<div className="activation-status-card">
				<span className="activation-status-card-icon">
					<ClayIcon symbol={config.icon} />
				</span>

				<div className="flex-grow-1">
					<span className='fw-bold'>{productName}</span>

					<p className="list-card-subtext m-0">
						{ACTIVATION_STATUS_MOCK.dateRange}
					</p>
				</div>

				<ClayLabel displayType={status.displayType}>
					{translate(status.label)}
				</ClayLabel>
			</div>
		</div>
	);
}
