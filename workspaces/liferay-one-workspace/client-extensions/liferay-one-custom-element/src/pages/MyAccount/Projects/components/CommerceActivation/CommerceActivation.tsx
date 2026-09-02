/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayTable from '@clayui/table';
import {useMemo} from 'react';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {useProject} from '~/context/ProjectContext';
import {useProperties} from '~/context/PropertiesContext';
import {LearnLinks} from '~/enums/Learn';
import {Word, translate} from '~/i18n';
import ActivationKeyDownload from '~/pages/MyAccount/Projects/components/ActivationKeyDownload/ActivationKeyDownload';
import requiresActivationKey from '~/pages/MyAccount/Projects/utils/requiresActivationKey';

type CommerceInstructionRow = {
	detail?: Word;
	instructions: Word;
	linkLabel?: Word;
	linkURL?: string;
	version: string;
};

export default function CommerceActivation() {
	const {project} = useProject();
	const {contactSupportURL} = useProperties();

	const commerceInstructions: CommerceInstructionRow[] = useMemo(
		() => [
			{
				instructions: 'all-commerce-modules-are-enabled-by-default',
				version: 'DXP 7.4 GA1+',
			},
			{
				detail: 'more-details',
				instructions: 'commerce-is-activated-using-a-portal-property',
				linkLabel: 'activating-liferay-commerce',
				linkURL: LearnLinks.ACTIVATING_LIFERAY_COMMERCE_ENTERPRISE,
				version: 'DXP 7.3 FP3/SP2+',
			},
			{
				detail: 'to-request-a-new-or-replacement-activation-key-please',
				instructions: 'commerce-requires-an-activation-key',
				linkLabel: 'open-a-support-ticket',
				linkURL: contactSupportURL,
				version: 'DXP 7.3 FP2/SP1',
			},
		],
		[contactSupportURL]
	);

	if (requiresActivationKey(project?.liferayVersion)) {
		return (
			<ActivationKeyDownload
				productGroup="COMMERCE"
				productTitle="Commerce"
			/>
		);
	}

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
					{commerceInstructions.map((row) => (
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
											{translate(row.detail)}{' '}
											{row.linkURL && row.linkLabel && (
												<a
													href={row.linkURL}
													rel="noopener noreferrer"
													target="_blank"
												>
													{translate(row.linkLabel)}
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
