/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTable from '@clayui/table';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import Button from '~/components/Button/Button';
import {Word, translate} from '~/i18n';
import {CLOUD_NATIVE_ENVIRONMENTS_MOCK} from '~/pages/MyAccount/Projects/utils/activationMockData';

import PopoverIcon from '../PopoverIcon/PopoverIcon';

export default function CloudNativeActivation() {
	return (
		<DetailedCard
			cardIconAltText={translate('cloud-native-environments')}
			cardTitle={translate('cloud-native-environments')}
			className="mt-3"
			clayIcon="cloud"
		>
			<div className="d-flex flex-column gap-4 mt-3">
				{CLOUD_NATIVE_ENVIRONMENTS_MOCK.map((subscription) => (
					<ClayTable borderless key={subscription.id}>
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									{translate('environment')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('subscription-id')}

									<PopoverIcon
										title={translate(
											'please-copy-and-paste-this-subscription-id-to-your-cloud-native-instance'
										)}
									/>
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('maximum-cluster-nodes')}

									<PopoverIcon
										title={translate(
											'maximum-number-of-active-nodes-available-for-this-environment'
										)}
									/>
								</ClayTable.Cell>

								<ClayTable.Cell
									className="text-center"
									headingCell
								>
									{translate('download')}
								</ClayTable.Cell>
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{subscription.rows.map((row) => (
								<ClayTable.Row key={row.environment}>
									<ClayTable.Cell>
										{translate(row.environment as Word)}
									</ClayTable.Cell>

									<ClayTable.Cell>
										{row.subscriptionId}
									</ClayTable.Cell>

									<ClayTable.Cell>
										{row.maxClusterNodes}
									</ClayTable.Cell>

									<ClayTable.Cell className="text-center">
										<Button
											aria-label={translate('download')}
											borderless
											className="text-neutral-7"
											displayType="unstyled"
											prependIcon="download"
										/>
									</ClayTable.Cell>
								</ClayTable.Row>
							))}
						</ClayTable.Body>
					</ClayTable>
				))}
			</div>
		</DetailedCard>
	);
}
