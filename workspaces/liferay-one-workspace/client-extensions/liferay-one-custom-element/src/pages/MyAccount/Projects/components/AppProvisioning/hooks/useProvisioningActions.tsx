/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useModal} from '@clayui/modal';
import {useRef, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useProperties} from '~/context/PropertiesContext';
import useModalContext from '~/hooks/useModalContext';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import Console from '~/services/spring-boot/Console';

import ProvisioningDetails from '../ProvisioningDetails/ProvisioningDetails';
import {InstallStatus} from '../types';

import type {Account} from '~/types/accounts';
import type {PlacedOrder} from '~/types/orders';

import type {ProvisioningData, ProvisioningRow} from './useProvisioningData';

type UseProvisioningActionsProps = {
	mutateOrder: ProvisioningData['mutateOrder'];
	order: PlacedOrder;
	resourceRequirements: ProvisioningData['resourceRequirements'];
	selectedAccount?: Account;
};

const getProvisioningStatus = (provisioningRow: ProvisioningRow) => ({
	inProgress: provisioningRow.status === InstallStatus.IN_PROGRESS,
	isExpired: provisioningRow.status === InstallStatus.EXPIRED,
	isInstalled: provisioningRow.status === InstallStatus.INSTALLED,
	readyToInstall: provisioningRow.status === InstallStatus.READY_TO_INSTALL,
});

const useProvisioningActions = ({
	mutateOrder,
	order,
	resourceRequirements,
	selectedAccount,
}: UseProvisioningActionsProps) => {
	const {cloudConsoleURL} = useProperties();
	const {onClose, onOpenModal} = useModalContext();
	const [loading, setLoading] = useState(false);
	const [selectedProvisioningRow, setSelectedProvisioningRow] =
		useState<ProvisioningRow>();
	const installAlertModal = useModal();
	const navigate = useNavigate();
	const uninstallModal = useModal();

	const onClickInstall = (provisioningRow: ProvisioningRow) => {
		setSelectedProvisioningRow(provisioningRow);

		if (!resourceRequirements.resourceRequest?.userProjects?.length) {
			return installAlertModal.onOpenChange(true);
		}

		navigate(`install/${order.id}`);
	};

	const uninstall = async (provisioningRow: ProvisioningRow) => {
		setLoading(true);

		try {
			await Console.uninstallApp(order.id, {
				id: provisioningRow.id,
				orderItemId: provisioningRow.orderItemId,
			});

			await mutateOrder();

			Liferay.Util.openToast({
				message: i18n.translate('your-request-completed-successfully'),
				type: 'success',
			});
		}
		catch (error) {
			console.warn(error);

			Liferay.Util.openToast({
				message: i18n.translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}

		setLoading(false);
	};

	const openUninstallModal = (provisioningRow: ProvisioningRow) => {
		setSelectedProvisioningRow(provisioningRow);

		uninstallModal.onOpenChange(true);
	};

	const onOpenDetailsModal = (provisioningRow: ProvisioningRow) => {
		const {inProgress, isInstalled, readyToInstall} =
			getProvisioningStatus(provisioningRow);

		onOpenModal({
			body: (
				<ProvisioningDetails
					account={selectedAccount}
					headerInfo={{
						image: order.placedOrderItems?.[0]?.thumbnail,
						licenseType: `${provisioningRow?.type} License for ${selectedAccount?.name}`,
						name: order.placedOrderItems?.[0]?.name,
					}}
					onClose={onClose}
					provisioningRow={provisioningRow}
				/>
			),
			center: true,
			footer: [
				undefined,
				undefined,
				<div key="details-footer-buttons">
					{readyToInstall && (
						<ClayButton
							className="border border-primary ml-2 rounded-lg text-primary"
							disabled={inProgress}
							displayType="secondary"
							onClick={() => {
								onClose();

								onClickInstall(provisioningRow);
							}}
							size="sm"
						>
							{i18n.translate('install')}
						</ClayButton>
					)}

					{isInstalled && (
						<ClayButton
							className="border border-danger ml-2 rounded-lg text-danger"
							displayType="secondary"
							onClick={() => {
								onClose();

								openUninstallModal(provisioningRow);
							}}
							size="sm"
						>
							{i18n.translate('uninstall')}
						</ClayButton>
					)}

					<ClayButton
						className="ml-2 rounded-lg"
						displayType="primary"
						onClick={onClose}
						size="sm"
					>
						{i18n.translate('done')}
					</ClayButton>
				</div>,
			],
			size: 'lg',
		});
	};

	const provisioningRef = useRef([
		{
			action: (provisioningRow: ProvisioningRow) =>
				onClickInstall(provisioningRow),
			show: (provisioningRow: ProvisioningRow) =>
				provisioningRow.status === InstallStatus.READY_TO_INSTALL,
			title: i18n.translate('install'),
		},
		{
			action: (provisioningRow: ProvisioningRow) =>
				onOpenDetailsModal(provisioningRow),
			show: () => true,
			title: i18n.translate('view-details'),
		},
		{
			action: (provisioningRow: ProvisioningRow) => {
				const projectId =
					`${provisioningRow.project}-${provisioningRow.environment}`.toLowerCase();

				window.open(
					`${cloudConsoleURL}/projects/${projectId}/services`
				);
			},
			show: (provisioningRow: ProvisioningRow) =>
				provisioningRow.status === InstallStatus.INSTALLED,
			title: i18n.translate('go-to-cloud-console'),
		},
		{
			action: (provisioningRow: ProvisioningRow) =>
				openUninstallModal(provisioningRow),
			show: (provisioningRow: ProvisioningRow) => {
				const {isInstalled} = getProvisioningStatus(provisioningRow);

				return isInstalled;
			},
			title: i18n.translate('uninstall'),
		},
	]);

	return {
		actions: provisioningRef.current,
		installAlertModal,
		loading,
		onOpenDetailsModal,
		selectedProvisioningRow,
		uninstall,
		uninstallModal,
	};
};

export default useProvisioningActions;
