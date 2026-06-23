/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {EXTEND_TRIAL_STATUS_LABEL} from '~/pages/Admin/SSADashboard/utils/constants';
import {ExtendRequestStatus} from '~/types/ssaDashboard';

import './ExtensionStatus.css';

type ExtensionStatusProps = {
	className?: string;
	extensionStatus?: keyof typeof EXTEND_TRIAL_STATUS_LABEL;
};

const ExtensionStatus = ({
	className,
	extensionStatus,
}: ExtensionStatusProps) => (
	<div className={className}>
		<span
			className={classNames('extension-status', {
				'extension-status-approved': [
					'Approved',
					'AutoApproved',
				].includes(extensionStatus as ExtendRequestStatus),
				'extension-status-expired': [
					'extension-expired',
					'Rejected',
				].includes(extensionStatus as ExtendRequestStatus),
				'extension-status-not-requested':
					extensionStatus === 'not-requested' || !extensionStatus,
				'extension-status-pending': extensionStatus === 'Pending',
			})}
		>
			{EXTEND_TRIAL_STATUS_LABEL[extensionStatus ?? 'not-requested']}
		</span>
	</div>
);
export default ExtensionStatus;
