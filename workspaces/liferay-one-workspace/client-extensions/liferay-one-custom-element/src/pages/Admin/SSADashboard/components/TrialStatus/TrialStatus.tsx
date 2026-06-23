/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import classNames from 'classnames';
import {TRIAL_STATUS_LABEL} from '~/pages/Admin/SSADashboard/utils/constants';

import './TrialStatus.css';

import type {OrderStatus} from '~/types/orders';

type TrialStatusProps = {
	trialStatus: string;
};

const TrialStatus = ({trialStatus}: TrialStatusProps) => {
	if ('processing' === trialStatus) {
		return (
			<span className="d-flex trial-status-text">
				<ClayLoadingIndicator
					className="m-0 mr-1"
					displayType="primary"
				/>
				{
					TRIAL_STATUS_LABEL[
						trialStatus as keyof typeof TRIAL_STATUS_LABEL
					]
				}
			</span>
		);
	}

	return (
		<>
			<ClayIcon
				className={classNames('mr-2 trial-status-icon', {
					'trial-status-icon-completed': [
						'approved',
						'cancelled',
						'completed',
					].includes(trialStatus as OrderStatus),
					'trial-status-icon-in_progress':
						'in-progress' === trialStatus,
					'trial-status-icon-on-hold': 'on-hold' === trialStatus,
					'trial-status-icon-pending': 'pending' === trialStatus,
					'trial-status-icon-processing':
						trialStatus === 'processing',
				})}
				symbol="circle"
			/>

			<span className="trial-status-text">
				{
					TRIAL_STATUS_LABEL[
						trialStatus as keyof typeof TRIAL_STATUS_LABEL
					]
				}
			</span>
		</>
	);
};

export default TrialStatus;
