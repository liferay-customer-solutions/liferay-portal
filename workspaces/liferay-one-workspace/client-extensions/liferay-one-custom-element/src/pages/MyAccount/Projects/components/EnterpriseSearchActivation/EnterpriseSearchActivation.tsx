/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClaySelect} from '@clayui/form';
import {format} from 'date-fns';
import {useMemo, useState} from 'react';
import useSWR from 'swr';
import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {translate} from '~/i18n';
import CommonLicenseKeys, {
	CommonLicenseKey,
} from '~/services/spring-boot/CommonLicenseKeys';

import type {APIResponse} from '~/types/api';

const GETTING_STARTED_URL = '';

function formatDate(value?: string): string {
	if (!value) {
		return '';
	}

	const date = new Date(value);

	return Number.isNaN(date.getTime()) ? value : format(date, 'MMM d, yyyy');
}

function termLabel(key: CommonLicenseKey): string {
	return `${formatDate(key.startDate)} - ${formatDate(key.endDate)}`;
}

export default function EnterpriseSearchActivation() {
	const {data} = useSWR<APIResponse<CommonLicenseKey>>(
		['common-license-keys', 'ENTERPRISE_SEARCH'],
		() =>
			CommonLicenseKeys.getCommonLicenseKeys({
				page: 1,
				pageSize: 100,
				productGroup: 'ENTERPRISE_SEARCH',
			})
	);

	const keys = useMemo(() => data?.items ?? [], [data]);

	const subscriptions = useMemo(
		() =>
			Array.from(
				new Set(keys.map((key) => key.productEnvironment))
			).sort(),
		[keys]
	);

	const [subscription, setSubscription] = useState('');

	const selectedSubscription = subscription || subscriptions[0] || '';

	const terms = useMemo(
		() =>
			keys.filter(
				(key) => key.productEnvironment === selectedSubscription
			),
		[keys, selectedSubscription]
	);

	const [termId, setTermId] = useState('');

	const selectedTerm =
		terms.find((key) => String(key.id) === termId) ?? terms[0];

	const handleDownload = () => {
		if (selectedTerm) {
			CommonLicenseKeys.downloadCommonLicenseKey(
				selectedTerm.id,
				selectedTerm.name
			);
		}
	};

	return (
		<DetailedCard
			cardIconAltText={translate('activation-keys')}
			cardTitle={translate('activation-keys')}
			className="mt-3"
			clayIcon="key"
		>
			<p className="mt-3 text-neutral-7">
				{translate(
					'select-an-active-enterprise-search-subscription-to-download-the-activation-key'
				)}
			</p>

			<div
				className="d-flex flex-wrap"
				style={{gap: 'var(--spacer-4)', maxWidth: '32rem'}}
			>
				<div className="flex-grow-1">
					<label htmlFor="enterprise-search-subscription">
						{translate('subscription')}
					</label>

					<ClaySelect
						id="enterprise-search-subscription"
						onChange={(event) => {
							setSubscription(event.target.value);
							setTermId('');
						}}
						value={selectedSubscription}
					>
						{subscriptions.map((option) => (
							<ClaySelect.Option
								key={option}
								label={option}
								value={option}
							/>
						))}
					</ClaySelect>
				</div>

				<div className="flex-grow-1">
					<label htmlFor="enterprise-search-term">
						{translate('subscription-term')}
					</label>

					<ClaySelect
						id="enterprise-search-term"
						onChange={(event) => setTermId(event.target.value)}
						value={selectedTerm ? String(selectedTerm.id) : ''}
					>
						{terms.map((key) => (
							<ClaySelect.Option
								key={key.id}
								label={termLabel(key)}
								value={String(key.id)}
							/>
						))}
					</ClaySelect>
				</div>
			</div>

			<Button
				className="mt-4"
				disabled={!selectedTerm}
				displayType="secondary"
				onClick={handleDownload}
				prependIcon="download"
			>
				{translate('download-key')}
			</Button>

			<p className="mt-4 text-neutral-7">
				{translate(
					'for-instructions-on-how-to-setup-your-software-read'
				)}{' '}
				<a href={GETTING_STARTED_URL} rel="noopener" target="_blank">
					{translate(
						'getting-started-with-liferay-enterprise-search-article'
					)}
				</a>
			</p>
		</DetailedCard>
	);
}
