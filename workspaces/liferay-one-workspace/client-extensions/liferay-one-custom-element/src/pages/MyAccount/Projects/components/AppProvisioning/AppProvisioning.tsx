/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useAccount} from '~/hooks/data/useAccounts';

import ProvisioningTable from './ProvisioningTable/ProvisioningTable';
import useProvisioningData from './hooks/useProvisioningData';

type AppProvisioningProps = {
	orderId: string;
};

export default function AppProvisioning({orderId}: AppProvisioningProps) {
	const {data: selectedAccount} = useAccount();

	const {mutateOrder, order, provisioningTableData, resourceRequirements} =
		useProvisioningData(orderId);

	return (
		<ProvisioningTable
			mutateOrder={mutateOrder}
			order={order}
			provisioningTableData={provisioningTableData}
			resourceRequirements={resourceRequirements}
			selectedAccount={selectedAccount}
		/>
	);
}
