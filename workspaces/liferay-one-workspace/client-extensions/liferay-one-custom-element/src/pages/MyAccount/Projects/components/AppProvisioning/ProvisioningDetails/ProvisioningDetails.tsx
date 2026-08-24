/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import ClaySticker from '@clayui/sticker';
import classNames from 'classnames';
import ButtonWithIcon from '~/components/ButtonWithIcon/ButtonWithIcon';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import {InstallStatus} from '../types';

import './ProvisioningDetails.css';

import type {Account} from '~/types/accounts';

import type {ProvisioningRow} from '../hooks/useProvisioningData';

type ProvisioningDetailsProps = {
	account?: Account;
	headerInfo?: {
		image?: string;
		licenseType?: string;
		name?: string;
	};
	onClose: () => void;
	provisioningRow: ProvisioningRow;
};

type InfoBadgeProps = {
	children?: string;
	status?: string;
	title: string;
};

const badgeStatus = {
	[InstallStatus.EXPIRED]: 'provisioning-details-info-badge-expired',
	[InstallStatus.IN_PROGRESS]:
		'provisioning-details-info-badge-ready-to-install',
	[InstallStatus.INSTALLED]: 'provisioning-details-info-badge-installed',
	[InstallStatus.READY_TO_INSTALL]:
		'provisioning-details-info-badge-ready-to-install',
};

const InfoBadge: React.FC<InfoBadgeProps> = ({children, status, title}) => (
	<div className="d-flex flex-column mb-4">
		<p className="font-weight-bold m-0 text-black-50">{title}</p>

		<div className="d-inline-flex">
			<div
				className={classNames(
					'font-weight-bold px-3 py-2 rounded-lg text-capitalize',
					{
						'provisioning-details-info-badge': !status,
					},
					status && badgeStatus[status as keyof typeof badgeStatus]
				)}
			>
				{children}
			</div>
		</div>
	</div>
);

const ProvisioningDetails: React.FC<ProvisioningDetailsProps> = ({
	account,
	headerInfo,
	onClose,
	provisioningRow,
}) => (
	<div className="d-flex flex-column mb-9 provisioning-details">
		<div className="align-items-center d-flex justify-content-between">
			<span className="font-weight-bold text-primary">
				{i18n.translate('provisioning-details').toUpperCase()}
			</span>

			<span>
				<ButtonWithIcon
					aria-label={i18n.translate('close')}
					borderless
					className="text-dark"
					displayType="unstyled"
					onClick={onClose}
					symbol="times"
					title={i18n.translate('close')}
				/>
			</span>
		</div>

		<div className="d-flex justify-content-between mb-5">
			<div className="align-items-center d-flex">
				{headerInfo?.image && (
					<img
						alt=""
						className="object-fit-cover rounded"
						draggable={false}
						height="48px"
						src={headerInfo.image}
						width="48px"
					/>
				)}

				<div className="d-flex flex-column ml-3">
					<strong>{headerInfo?.name}</strong>

					<span className="text-black-50">
						{headerInfo?.licenseType}
					</span>
				</div>
			</div>

			<div className="align-items-center d-flex">
				<div className="align-items-end d-flex flex-column mx-2">
					<strong>{account?.name}</strong>

					<div className="text-black-50">
						{Liferay.ThemeDisplay.getUserEmailAddress()}
					</div>
				</div>

				<ClaySticker displayType="light" shape="circle" size="sm">
					{account?.logoURL ? (
						<ClaySticker.Image
							alt=""
							draggable={false}
							height={24}
							src={account.logoURL}
							width={24}
						/>
					) : (
						<ClayIcon symbol="picture" />
					)}
				</ClaySticker>
			</div>
		</div>

		<div className="d-flex flex-row mb-7 mt-5">
			<div className="col-6 p-0">
				<p className="font-weight-bold">
					{i18n.translate('client-extension')}
				</p>

				<InfoBadge title={i18n.translate('start-date')}>
					{provisioningRow?.startDate}
				</InfoBadge>

				<InfoBadge title={i18n.translate('expiration-date')}>
					{provisioningRow?.expirationDate}
				</InfoBadge>
			</div>

			<div className="col-6 p-0">
				<p className="font-weight-bold">
					{i18n.translate('installation-status')}
				</p>

				<InfoBadge
					status={provisioningRow?.status}
					title={i18n.translate('status')}
				>
					{i18n.translate(provisioningRow.status)}
				</InfoBadge>

				<InfoBadge title={i18n.translate('project')}>
					{provisioningRow.project || i18n.translate('not-installed')}
				</InfoBadge>

				<InfoBadge title={i18n.translate('environment')}>
					{provisioningRow.environment ||
						i18n.translate('not-installed')}
				</InfoBadge>
			</div>
		</div>
	</div>
);

export default ProvisioningDetails;
