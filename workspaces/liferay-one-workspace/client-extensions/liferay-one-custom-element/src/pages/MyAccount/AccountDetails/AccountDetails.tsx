/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import Page from '~/components/Page/Page';
import QATable, {Orientation} from '~/components/QATable/QATable';
import {useOneContext} from '~/context/OneContextProvider';
import useAccountDetails from '~/hooks/useAccountDetails';
import i18n from '~/i18n';

import './AccountDetails.css';

import type {Account, AccountPostalAddresses} from '~/types/accounts';

function formatPostalAddress(address?: AccountPostalAddresses) {
	if (!address) {
		return '-';
	}

	return (
		[
			address.streetAddressLine1,
			address.streetAddressLine2,
			address.addressLocality,
			[address.addressRegion, address.postalCode]
				.filter(Boolean)
				.join(' '),
			address.addressCountry,
		]
			.filter(Boolean)
			.join(', ') || '-'
	);
}

function getCustomFieldValue(account: Account | undefined, name: string) {
	const data = account?.customFields?.find(
		(customField) => customField.name === name
	)?.customValue?.data;

	return Array.isArray(data) ? data.join(', ') : data;
}

function textWrapper(content?: string | number) {
	return <p className="mb-0 mt-1">{content || '-'}</p>;
}

export default function AccountDetails() {
	const {myUserAccount} = useOneContext();

	const {data, error, isLoading} = useAccountDetails();

	const {account, postalAddresses} = data || {};

	const primaryAddress =
		postalAddresses?.items.find((address) => address.primary) ||
		postalAddresses?.items[0];

	const contactName = myUserAccount?.name;
	const contactEmail = myUserAccount?.emailAddress;
	const contactPhone =
		myUserAccount?.userAccountContactInformation?.telephones?.[0]
			?.phoneNumber;

	const accountInitial = account?.name?.charAt(0) ?? '';

	return (
		<Page
			description={i18n.translate(
				'manage-your-account-and-organization-details'
			)}
			pageRendererProps={{error, isLoading}}
			rightButton={
				<Button displayType="secondary" prependIcon="pencil">
					{i18n.translate('edit')}
				</Button>
			}
			title={i18n.translate('account-details')}
		>
			<div className="account-details-grid mt-4">
				<DetailedCard
					cardIconAltText={i18n.translate('main-information')}
					cardTitle={i18n.translate('main-information')}
					clayIcon="info-circle-open"
				>
					<div className="account-details-identity align-items-center d-flex mb-4 mt-3">
						{account?.logoURL ? (
							<img
								alt={account.name}
								className="account-details-identity-avatar"
								src={account.logoURL}
							/>
						) : (
							<span className="account-details-identity-avatar align-items-center d-flex justify-content-center">
								{accountInitial}
							</span>
						)}

						<div>
							<p className="account-details-identity-name">
								{account?.name || '-'}
							</p>

							<p className="account-details-identity-contact">
								{[contactName, contactEmail]
									.filter(Boolean)
									.join('  ')}
							</p>
						</div>
					</div>

					<span className="badge badge-primary mb-4">
						{i18n.translate('organization')}
					</span>

					<QATable
						items={[
							{
								title: i18n.translate('company-name'),
								value: textWrapper(account?.name),
							},
							{
								title: i18n.translate('company-description'),
								value: textWrapper(account?.description),
							},
							{
								title: i18n.translate('industry'),
								value: textWrapper(
									getCustomFieldValue(account, 'Industry')
								),
							},
							{
								title: i18n.translate('address'),
								value: textWrapper(
									formatPostalAddress(primaryAddress)
								),
							},
							{
								title: i18n.translate('account-id'),
								value: textWrapper(
									account?.externalReferenceCode
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>

				<div className="account-details-side">
					<DetailedCard
						cardIconAltText={i18n.translate('contacts')}
						cardTitle={i18n.translate('contacts')}
						clayIcon="users"
					>
						<p className="account-details-subheading mt-3">
							{i18n.translate('primary-contact')}
						</p>

						<QATable
							items={[
								{
									title: i18n.translate('name'),
									value: textWrapper(contactName),
								},
								{
									title: i18n.translate('email'),
									value: textWrapper(contactEmail),
								},
								{
									title: i18n.translate('phone'),
									value: textWrapper(contactPhone),
								},
							]}
							orientation={Orientation.VERTICAL}
						/>
					</DetailedCard>

					<DetailedCard
						cardIconAltText={i18n.translate('security')}
						cardTitle={i18n.translate('security')}
						clayIcon="lock"
					>
						<p className="account-details-subheading mt-3">
							{i18n.translate('settings')}
						</p>

						<QATable
							items={[
								{
									title: i18n.translate('password-policy'),
									value: textWrapper(
										getCustomFieldValue(
											account,
											'Password Policy'
										)
									),
								},
								{
									title: i18n.translate('okta-sso'),
									value: textWrapper(
										getCustomFieldValue(account, 'Okta SSO')
									),
								},
								{
									title: i18n.translate(
										'two-factor-authentication'
									),
									value: textWrapper(
										getCustomFieldValue(
											account,
											'Two-Factor Authentication'
										)
									),
								},
							]}
							orientation={Orientation.VERTICAL}
						/>
					</DetailedCard>
				</div>
			</div>
		</Page>
	);
}
