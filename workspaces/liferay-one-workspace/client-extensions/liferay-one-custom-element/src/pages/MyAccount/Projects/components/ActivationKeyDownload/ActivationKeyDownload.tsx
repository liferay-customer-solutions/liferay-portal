/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClaySelect} from '@clayui/form';
import {format} from 'date-fns';
import {useEffect, useMemo, useState} from 'react';
import useSWR from 'swr';
import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import {useOneContext} from '~/context/OneContextProvider';
import {useProject} from '~/context/ProjectContext';
import {useProperties} from '~/context/PropertiesContext';
import {sub, translate} from '~/i18n';
import {ACCOUNT_REQUESTER} from '~/pages/MyAccount/AccountMembers/accountRoles';
import FetcherError from '~/services/fetcher/FetcherError';
import {Liferay} from '~/services/liferay/liferay';
import CommonLicenseKeys, {
	CommonLicenseKey,
	ProductGroup,
} from '~/services/spring-boot/CommonLicenseKeys';
import {getFileNameExtension} from '~/utils/downloadFileUtils';
import getKebabCase from '~/utils/getKebabCase';
import {toAlphanumericLowerCase} from '~/utils/stringUtils';

import type {APIResponse} from '~/types/api';

type ActivationKeyDownloadProps = {
	productGroup: ProductGroup;
	productTitle: 'Commerce' | 'Enterprise Search';
};

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

export default function ActivationKeyDownload({
	productGroup,
	productTitle,
}: ActivationKeyDownloadProps) {
	const {project} = useProject();
	const {userAccountModel} = useOneContext();
	const {
		articleGettingStartedWithLiferayEnterpriseSearchURL,
		contactSupportURL,
	} = useProperties();

	const kebabProductTitle = getKebabCase(productTitle) ?? '';

	const {data, error} = useSWR<APIResponse<CommonLicenseKey>>(
		['common-license-keys', productGroup],
		() =>
			CommonLicenseKeys.getCommonLicenseKeys({
				page: 1,
				pageSize: 100,
				productGroup,
			})
	);

	const notEntitled = (error as FetcherError)?.status === 403;

	useEffect(() => {
		if (error && !notEntitled) {
			Liferay.Util.openToast({
				message: translate('unable-to-load-the-activation-keys'),
				type: 'danger',
			});
		}
	}, [error, notEntitled]);

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

	const [hasDownloadError, setHasDownloadError] = useState(false);

	useEffect(() => {
		setHasDownloadError(false);
	}, [selectedSubscription, selectedTerm?.id]);

	const handleDownload = async () => {
		if (!selectedTerm) {
			return;
		}

		try {
			await CommonLicenseKeys.downloadCommonLicenseKey(
				`activation-key-${[productTitle, selectedSubscription]
					.map(toAlphanumericLowerCase)
					.join('')}-${toAlphanumericLowerCase(
					project?.name ?? ''
				)}${getFileNameExtension(selectedTerm.name)}`,
				selectedTerm.id
			);
		}
		catch {
			setHasDownloadError(true);
		}
	};

	const isRequesterOrAdministrator = Boolean(
		userAccountModel?.isAccountAdministrator ||
			userAccountModel?.hasAccountRoleName(ACCOUNT_REQUESTER)
	);

	return (
		<DetailedCard
			cardIconAltText={translate('activation-keys')}
			cardTitle={translate('activation-keys')}
			className="mt-3"
			clayIcon="key"
		>
			{notEntitled ? (
				<p className="mt-3 text-neutral-7">
					{sub(
						'you-do-not-have-an-active-liferay-x-subscription',
						kebabProductTitle
					)}
				</p>
			) : (
				<>
					<p className="mt-3 text-neutral-7">
						{sub(
							'select-an-active-liferay-x-subscription-to-download-the-activation-key',
							kebabProductTitle
						)}
						.
					</p>

					<div
						className="d-flex flex-wrap"
						style={{gap: 'var(--spacer-4)', maxWidth: '32rem'}}
					>
						<div className="flex-grow-1">
							<label htmlFor={`${kebabProductTitle}-subscription`}>
								{translate('subscription')}
							</label>

							<ClaySelect
								id={`${kebabProductTitle}-subscription`}
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
							<label htmlFor={`${kebabProductTitle}-term`}>
								{translate('subscription-term')}
							</label>

							<ClaySelect
								id={`${kebabProductTitle}-term`}
								onChange={(event) =>
									setTermId(event.target.value)
								}
								value={
									selectedTerm ? String(selectedTerm.id) : ''
								}
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
						disabled={hasDownloadError || !selectedTerm}
						displayType="secondary"
						onClick={handleDownload}
						prependIcon="download"
					>
						{translate('download-key')}
					</Button>

					{hasDownloadError && (
						<p className="mt-3">
							<span className="text-danger">
								{sub(
									'the-requested-activation-key-is-not-yet-available',
									kebabProductTitle
								)}
							</span>

							{isRequesterOrAdministrator ? (
								<>
									{sub(
										'for-more-information-about-the-availability-of-your-x-activation-keys-please',
										kebabProductTitle
									)}

									<a
										href={contactSupportURL}
										rel="noreferrer"
										target="_blank"
									>
										<u className="font-weight-bold text-neutral-9">
											{` ${translate(
												'contact-the-support-team'
											)}`}
										</u>
									</a>
								</>
							) : (
								<span className="text-neutral-7">
									{sub(
										'if-you-need-more-information-about-the-availability-of-your-x-activation-keys-please-ask-one-of-your-administrator-team-members-to-update-your-permissions-so-you-can-contact-liferay-support-alternatively-team-members-with-administrator-or-requester-role-can-submit-a-support-ticket-on-your-behalf',
										kebabProductTitle
									)}
								</span>
							)}
						</p>
					)}

					{productGroup === 'ENTERPRISE_SEARCH' && (
						<p className="mt-4 text-neutral-7">
							{`${translate(
								'for-instructions-on-how-to-setup-your-liferay-enterprise-search-software-please-read-the'
							)} `}

							<a
								href={
									articleGettingStartedWithLiferayEnterpriseSearchURL
								}
								rel="noopener noreferrer"
								target="_blank"
							>
								{translate(
									'getting-started-with-liferay-enterprise-search-article'
								)}
							</a>
						</p>
					)}
				</>
			)}
		</DetailedCard>
	);
}
