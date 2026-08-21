/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useOutletContext} from 'react-router-dom';

import {OrderCustomFields} from '../../../../../enums/Order';
import useGetProductByOrderId from '../../../../../hooks/useGetProductByOrderId';
import {safeJSONParse} from '../../../../../utils/util';
import LDPTokenCard from '../Details/LDPTokenCard';
import WorkspaceInfoCard from '../Details/WorkspaceInfoCard';

type OutletContext = NonNullable<
	ReturnType<typeof useGetProductByOrderId>['data']
>;

const LDPWorkspace = () => {
	const {placedOrder} = useOutletContext<OutletContext>();

	const orderMetadata = safeJSONParse<any>(
		placedOrder.customFields[OrderCustomFields.ORDER_METADATA],
		{}
	);

	const {analyticsProject} = orderMetadata;

	return (
		<div className="app-details-body-container mt-4">
			<LDPTokenCard
				dataSourceAccessToken={analyticsProject?.dataSourceAccessToken}
				groupId={analyticsProject?.groupId}
			/>

			<WorkspaceInfoCard analyticsProject={analyticsProject} />
		</div>
	);
};

export default LDPWorkspace;
