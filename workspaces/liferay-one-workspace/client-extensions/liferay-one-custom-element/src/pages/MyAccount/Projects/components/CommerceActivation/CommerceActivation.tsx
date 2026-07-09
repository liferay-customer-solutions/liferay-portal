/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTable from '@clayui/table';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {translate} from '~/i18n';
import {COMMERCE_INSTRUCTIONS_MOCK} from '~/pages/MyAccount/Projects/utils/activationMockDataConstants';

export default function CommerceActivation() {
	return (
		<DetailedCard
			cardIconAltText={translate('activation-keys')}
			cardTitle={translate('activation-keys')}
			className="mt-3"
			clayIcon="key"
		>
			<ClayTable borderless className="mt-3">
				<ClayTable.Head>
					<ClayTable.Row>
						<ClayTable.Cell headingCell>
							{translate('version')}
						</ClayTable.Cell>

						<ClayTable.Cell expanded headingCell>
							{translate('instructions')}
						</ClayTable.Cell>
					</ClayTable.Row>
				</ClayTable.Head>

				<ClayTable.Body>
					{COMMERCE_INSTRUCTIONS_MOCK.map((row) => (
						<ClayTable.Row key={row.version}>
							<ClayTable.Cell>
								<span className="text-neutral-7">
									{row.version}
								</span>
							</ClayTable.Cell>

							<ClayTable.Cell expanded>
								<span className="d-flex flex-column">
									<span>{row.instructions}</span>

									{row.detail && (
										<span className="list-card-subtext">
											{row.detail}

											{row.detailLink && (
												<a
													href={row.detailLink.url}
													rel="noopener"
													target="_blank"
												>
													{row.detailLink.label}
												</a>
											)}
										</span>
									)}
								</span>
							</ClayTable.Cell>
						</ClayTable.Row>
					))}
				</ClayTable.Body>
			</ClayTable>
		</DetailedCard>
	);
}
