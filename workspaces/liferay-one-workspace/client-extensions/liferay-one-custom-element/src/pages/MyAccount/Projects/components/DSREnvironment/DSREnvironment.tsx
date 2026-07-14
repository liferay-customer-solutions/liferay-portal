/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import CopyTokenField from '~/components/CopyTokenField/CopyTokenField';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n from '~/i18n';

import DetailsCard, {DetailsRow} from '../DetailsCard/DetailsCard';

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';

type DSREnvironmentProps = {
	environment: ProjectEnvironment;
};

export default function DSREnvironment({environment}: DSREnvironmentProps) {
	const rows: DetailsRow[] = [
		{
			label: i18n.translate('workspace-name'),
			value: environment.workspaceName,
		},
		{
			label: i18n.translate('workspace-owner-email'),
			value: environment.ownerEmailAddress,
		},
		{
			label: i18n.translate('data-center-location'),
			value: environment.region,
		},
	].filter((row) => row.value);

	return (
		<>
			<DetailedCard
				cardIconAltText={i18n.translate('connect-your-liferay-dsr')}
				cardTitle={i18n.translate('connect-your-liferay-dsr')}
				className="mt-3"
				clayIcon="diagram"
				fitContent
			>
				<div className="mt-3">
					<p className="font-weight-semi-bold">
						{i18n.translate(
							'copy-this-token-to-your-liferay-dxp-instance'
						)}
					</p>

					<CopyTokenField
						token={environment.currentEntitlementHash}
					/>
				</div>
			</DetailedCard>

			<DetailsCard icon="document" rows={rows} title="workspace-info" />
		</>
	);
}
