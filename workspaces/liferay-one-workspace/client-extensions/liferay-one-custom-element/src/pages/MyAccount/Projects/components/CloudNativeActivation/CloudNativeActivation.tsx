/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTable from '@clayui/table';
import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import {Word, translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import PopoverIcon from '../PopoverIcon/PopoverIcon';

const CLOUD_TYPES = ['CNE', 'PaaS', 'SaaS'];

export default function CloudNativeActivation() {
	const {environments, loading} = useProjectEnvironments();

	const cloudEnvironments = environments.filter((environment) =>
		CLOUD_TYPES.includes(environment.type)
	);

	return (
		<DetailedCard
			cardIconAltText={translate('cloud-native-environments')}
			cardTitle={translate('cloud-native-environments')}
			className="mt-3"
			clayIcon="cloud"
		>
			{loading ? (
				<div className="p-4 text-neutral-7">{translate('loading')}</div>
			) : cloudEnvironments.length ? (
				<ClayTable borderless className="mt-3">
					<ClayTable.Head>
						<ClayTable.Row>
							<ClayTable.Cell headingCell>
								{translate('environment')}
							</ClayTable.Cell>

							<ClayTable.Cell headingCell>
								{translate('region')}
							</ClayTable.Cell>

							<ClayTable.Cell headingCell>
								{translate('identity')}

								<PopoverIcon
									title={translate(
										'please-copy-and-paste-this-subscription-id-to-your-cloud-native-instance'
									)}
								/>
							</ClayTable.Cell>

							<ClayTable.Cell headingCell>
								{translate('status')}
							</ClayTable.Cell>

							<ClayTable.Cell className="text-center" headingCell>
								{translate('download')}
							</ClayTable.Cell>
						</ClayTable.Row>
					</ClayTable.Head>

					<ClayTable.Body>
						{cloudEnvironments.map((environment) => (
							<ClayTable.Row
								key={environment.externalReferenceCode}
							>
								<ClayTable.Cell>
									<span className="d-flex flex-column">
										<span className="fw-bold">
											{environment.externalReferenceCode}
										</span>

										<span className="list-card-subtext">
											{environment.type}
										</span>
									</span>
								</ClayTable.Cell>

								<ClayTable.Cell>
									{environment.region || '-'}
								</ClayTable.Cell>

								<ClayTable.Cell>
									{environment.currentEntitlementHash || '-'}
								</ClayTable.Cell>

								<ClayTable.Cell>
									<span className="list-card-status">
										<span
											className="list-card-status-dot"
											style={{
												backgroundColor: getStatusColor(
													environment.status
												),
											}}
										/>

										{translate(environment.status as Word)}
									</span>
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
			) : (
				<div className="p-4 text-neutral-7">
					{translate('no-cloud-native-environments-yet')}
				</div>
			)}
		</DetailedCard>
	);
}
