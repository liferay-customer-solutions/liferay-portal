/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import ButtonWithIcon from '~/components/ButtonWithIcon/ButtonWithIcon';
import Loading from '~/components/Loading/Loading';
import Table from '~/components/Table/Table';
import i18n from '~/i18n';

import InstallAlertModal from '../InstallAlertModal/InstallAlertModal';
import InstallationStatus from '../InstallationStatus/InstallationStatus';
import UninstallModal from '../UninstallModal/UninstallModal';
import useProvisioningActions from '../hooks/useProvisioningActions';
import {InstallStatus} from '../types';

import type {Account} from '~/types/accounts';

import type {
	ProvisioningData,
	ProvisioningRow,
} from '../hooks/useProvisioningData';

type ProvisioningTableProps = ProvisioningData & {
	selectedAccount?: Account;
};

const ProvisioningTable: React.FC<ProvisioningTableProps> = ({
	mutateOrder,
	order,
	provisioningTableData,
	resourceRequirements,
	selectedAccount,
}) => {
	const {
		actions,
		installAlertModal,
		loading,
		onOpenDetailsModal,
		selectedProvisioningRow,
		uninstall,
		uninstallModal,
	} = useProvisioningActions({
		mutateOrder,
		order,
		resourceRequirements,
		selectedAccount,
	});

	return (
		<>
			<Table
				Actions={({row}) => {
					const provisioningRow = row as unknown as ProvisioningRow;

					return (
						<ClayDropDown
							trigger={
								<ButtonWithIcon
									aria-label={i18n.translate('actions')}
									displayType={null}
									symbol="ellipsis-v"
									title={i18n.translate('actions')}
								/>
							}
						>
							<ClayDropDown.ItemList>
								{actions
									.filter((action) =>
										action.show(provisioningRow)
									)
									.map((action, index) => (
										<ClayDropDown.Item
											key={index}
											onClick={() =>
												action.action(provisioningRow)
											}
										>
											{action.title}
										</ClayDropDown.Item>
									))}
							</ClayDropDown.ItemList>
						</ClayDropDown>
					);
				}}
				className="mt-4"
				columns={[
					{
						key: 'type',
						render: (type, item) => {
							const provisioningRow =
								item as unknown as ProvisioningRow;

							return (
								<>
									<div className="font-weight-bold">
										{type as string}
									</div>

									<div>{provisioningRow.host}</div>
								</>
							);
						},
						title: (
							<>
								<div className="text-dark">
									{i18n.translate('type')}
								</div>

								<div className="text-black-50">
									{i18n.translate('host-name')}
								</div>
							</>
						),
					},
					{
						key: 'startDate',
						render: (startDate, item) => {
							const provisioningRow =
								item as unknown as ProvisioningRow;

							return (
								<>
									<div>{startDate as string}</div>

									<div>{provisioningRow.expirationDate}</div>
								</>
							);
						},
						title: (
							<>
								<div className="text-dark">
									{i18n.translate('start-date')}
								</div>

								<div className="text-dark">
									{i18n.translate('exp-date')}
								</div>
							</>
						),
					},
					{
						key: 'status',
						render: (status, item) => {
							const provisioningRow =
								item as unknown as ProvisioningRow;

							return (
								<div className="align-items-center d-flex">
									<InstallationStatus
										status={status as string}
									>
										{status as string}
									</InstallationStatus>

									{provisioningRow.status ===
										InstallStatus.IN_PROGRESS && (
										<Loading
											displayType="primary"
											shape="circle"
											size="sm"
										/>
									)}
								</div>
							);
						},
						title: (
							<div className="text-dark">
								{i18n.translate('status')}
							</div>
						),
					},
					{
						key: 'project',
						render: (project, item) => {
							const provisioningRow =
								item as unknown as ProvisioningRow;

							return (
								<>
									<div className="font-weight-bold">
										{(project as string) ||
											i18n.translate('not-installed')}
									</div>

									<div>
										{provisioningRow.environment ||
											i18n.translate('not-installed')}
									</div>
								</>
							);
						},
						title: (
							<>
								<div className="text-dark">
									{i18n.translate('project')}
								</div>

								<div className="text-black-50">
									{i18n.translate('environment')}
								</div>
							</>
						),
					},
				]}
				hasKebabButton
				onClickRow={(row) =>
					onOpenDetailsModal(row as unknown as ProvisioningRow)
				}
				rows={
					provisioningTableData as unknown as Record<
						string,
						unknown
					>[]
				}
			/>

			{selectedProvisioningRow && (
				<InstallAlertModal modal={installAlertModal} />
			)}

			{selectedProvisioningRow && (
				<UninstallModal
					loading={loading}
					modal={uninstallModal}
					provisioningRow={selectedProvisioningRow}
					uninstall={uninstall}
				/>
			)}
		</>
	);
};

export default ProvisioningTable;
