/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import './AIHubEnvironment.css';

import type {ProjectEnvironment} from '~/hooks/useProjectEnvironments';

type AIHubEnvironmentProps = {
	environment: ProjectEnvironment;
};

type Cell = {
	label: string;
	value: ReactNode;
};

export default function AIHubEnvironment({environment}: AIHubEnvironmentProps) {
	const accountName = Liferay.CommerceContext.account?.accountName ?? '';

	const aiHubURL = environment.aiHubURL;

	const href = aiHubURL.startsWith('http') ? aiHubURL : `https://${aiHubURL}`;

	const cells: Cell[] = [
		{label: i18n.translate('ai-hub-account-name'), value: accountName},
		{
			label: i18n.translate('administration-email'),
			value: environment.ownerEmailAddress,
		},
		{
			label: i18n.translate('token-monthly-allowance'),
			value: environment.tokenMonthlyAllowance
				? i18n.sub('x-tokens', environment.tokenMonthlyAllowance)
				: '',
		},
		{
			label: i18n.translate('ai-hub-url'),
			value: aiHubURL ? (
				<a href={href} rel="noopener noreferrer" target="_blank">
					{aiHubURL}
				</a>
			) : (
				''
			),
		},
	].filter((cell) => cell.value);

	return (
		<DetailedCard
			cardIconAltText={i18n.translate('ai-hub-details')}
			cardTitle={i18n.translate('ai-hub-details')}
			className="mt-3"
			clayIcon="order-form-tag"
			fitContent
		>
			<div className="ai-hub-environment-grid">
				{cells.map((cell) => (
					<div className="ai-hub-environment-cell" key={cell.label}>
						<span className="ai-hub-environment-cell-label">
							{cell.label}
						</span>

						<span className="ai-hub-environment-cell-value">
							{cell.value}
						</span>
					</div>
				))}
			</div>
		</DetailedCard>
	);
}
