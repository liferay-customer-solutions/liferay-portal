/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTable from '@clayui/table';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {Word, translate} from '~/i18n';

type CommerceInstructionRow = {
	detail?: Word;
	instructions: Word;
	version: string;
};

const COMMERCE_INSTRUCTIONS: CommerceInstructionRow[] = [
	{
		instructions: 'all-commerce-modules-are-enabled-by-default',
		version: 'DXP 7.4 GA1+',
	},
	{
		detail: 'commerce-is-activated-using-a-portal-property-see-the-documentation-for-details',
		instructions: 'commerce-is-activated-using-a-portal-property',
		version: 'DXP 7.3 FP3/SP2+',
	},
	{
		detail: 'to-request-a-new-or-replacement-activation-key-open-a-support-ticket',
		instructions: 'commerce-requires-an-activation-key',
		version: 'DXP 7.3 FP2/SP1',
	},
];

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
					{COMMERCE_INSTRUCTIONS.map((row) => (
						<ClayTable.Row key={row.version}>
							<ClayTable.Cell>
								<span className="text-neutral-7">
									{row.version}
								</span>
							</ClayTable.Cell>

							<ClayTable.Cell expanded>
								<span className="d-flex flex-column">
									<span>{translate(row.instructions)}</span>

									{row.detail && (
										<span className="list-card-subtext">
											{translate(row.detail)}
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
