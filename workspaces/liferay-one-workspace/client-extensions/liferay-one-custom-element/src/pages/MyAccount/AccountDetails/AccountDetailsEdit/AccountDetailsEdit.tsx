/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayForm, {ClayInput} from '@clayui/form';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import Page from '~/components/Page/Page';
import useAccountDetails from '~/hooks/useAccountDetails';
import i18n, {Word} from '~/i18n';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';

import '../AccountDetails.css';

import type {AccountPostalAddresses} from '~/types/accounts';

type CustomFieldConfig = {
	label: Word;
	name: string;
};

type AddressFieldConfig = {
	key: keyof AccountPostalAddresses;
	label: Word;
};

const CUSTOM_FIELDS: CustomFieldConfig[] = [
	{label: 'industry', name: 'Industry'},
	{label: 'password-policy', name: 'Password Policy'},
	{label: 'okta-sso', name: 'Okta SSO'},
	{label: 'two-factor-authentication', name: 'Two-Factor Authentication'},
];

const ADDRESS_FIELDS: AddressFieldConfig[] = [
	{key: 'streetAddressLine1', label: 'street-address'},
	{key: 'addressLocality', label: 'city'},
	{key: 'addressRegion', label: 'state'},
	{key: 'postalCode', label: 'postal-code'},
	{key: 'addressCountry', label: 'country'},
];

export default function AccountDetailsEdit() {
	const navigate = useNavigate();

	const {data, error: loadError, isLoading, mutate} = useAccountDetails();

	const {account, postalAddresses} = data || {};

	const primaryAddress =
		postalAddresses?.items.find((address) => address.primary) ||
		postalAddresses?.items[0];

	const [name, setName] = useState('');
	const [description, setDescription] = useState('');
	const [customValues, setCustomValues] = useState<Record<string, string>>(
		{}
	);
	const [address, setAddress] = useState<Record<string, string>>({});
	const [error, setError] = useState<string>();
	const [saving, setSaving] = useState(false);

	useEffect(() => {
		if (account) {
			setName(account.name ?? '');
			setDescription(account.description ?? '');

			setCustomValues(
				CUSTOM_FIELDS.reduce<Record<string, string>>(
					(accumulator, {name: fieldName}) => {
						const fieldData = account.customFields?.find(
							(customField) => customField.name === fieldName
						)?.customValue?.data;

						accumulator[fieldName] = Array.isArray(fieldData)
							? fieldData.join(', ')
							: fieldData ?? '';

						return accumulator;
					},
					{}
				)
			);
		}
	}, [account]);

	useEffect(() => {
		if (primaryAddress) {
			setAddress(
				ADDRESS_FIELDS.reduce<Record<string, string>>(
					(accumulator, {key}) => {
						accumulator[key] = String(primaryAddress[key] ?? '');

						return accumulator;
					},
					{}
				)
			);
		}
	}, [primaryAddress]);

	const onSave = async () => {
		if (!account?.id) {
			setError(i18n.translate('unable-to-update-account-details'));

			return;
		}

		setError(undefined);
		setSaving(true);

		try {
			const existingCustomFieldNames = new Set(
				(account.customFields ?? []).map(
					(customField) => customField.name
				)
			);

			await HeadlessAdminUser.updateAccount(account.id, {
				customFields: CUSTOM_FIELDS.filter(({name: fieldName}) =>
					existingCustomFieldNames.has(fieldName)
				).map(({name: fieldName}) => ({
					customValue: {data: customValues[fieldName] ?? ''},
					name: fieldName,
				})),
				description,
				name,
			});

			if (primaryAddress?.id) {
				await HeadlessAdminUser.updateAccountPostalAddress(
					primaryAddress.id,
					ADDRESS_FIELDS.reduce<Record<string, string>>(
						(accumulator, {key}) => {
							accumulator[key] = address[key] ?? '';

							return accumulator;
						},
						{}
					)
				);
			}

			await mutate();

			navigate('..');
		}
		catch (requestError) {
			setError(
				(requestError as Error)?.message ??
					i18n.translate('unable-to-update-account-details')
			);
		}
		finally {
			setSaving(false);
		}
	};

	return (
		<Page
			description={i18n.translate(
				'manage-your-account-and-organization-details'
			)}
			pageRendererProps={{error: loadError, isLoading}}
			rightButton={
				<div className="d-flex" style={{gap: 'var(--spacer-2)'}}>
					<Button
						disabled={saving}
						displayType="secondary"
						onClick={() => navigate('..')}
					>
						{i18n.translate('cancel')}
					</Button>

					<Button
						displayType="primary"
						isLoading={saving}
						onClick={onSave}
					>
						{i18n.translate('save')}
					</Button>
				</div>
			}
			title={i18n.translate('edit-account-details')}
		>
			{error && (
				<ClayAlert displayType="danger" title={i18n.translate('error')}>
					{error}
				</ClayAlert>
			)}

			<div className="account-details-grid mt-4">
				<DetailedCard
					cardIconAltText={i18n.translate('main-information')}
					cardTitle={i18n.translate('main-information')}
					clayIcon="info-circle-open"
				>
					<div className="mt-3">
						<ClayForm.Group>
							<label htmlFor="account-name">
								{i18n.translate('company-name')}
							</label>

							<ClayInput
								id="account-name"
								onChange={(event) =>
									setName(event.target.value)
								}
								type="text"
								value={name}
							/>
						</ClayForm.Group>

						<ClayForm.Group>
							<label htmlFor="account-description">
								{i18n.translate('company-description')}
							</label>

							<ClayInput
								component="textarea"
								id="account-description"
								onChange={(event) =>
									setDescription(event.target.value)
								}
								value={description}
							/>
						</ClayForm.Group>

						<ClayForm.Group>
							<label htmlFor="account-Industry">
								{i18n.translate('industry')}
							</label>

							<ClayInput
								id="account-Industry"
								onChange={(event) =>
									setCustomValues((previous) => ({
										...previous,
										Industry: event.target.value,
									}))
								}
								type="text"
								value={customValues.Industry ?? ''}
							/>
						</ClayForm.Group>

						{ADDRESS_FIELDS.map(({key, label}) => (
							<ClayForm.Group key={key}>
								<label htmlFor={`account-${key}`}>
									{i18n.translate(label)}
								</label>

								<ClayInput
									id={`account-${key}`}
									onChange={(event) =>
										setAddress((previous) => ({
											...previous,
											[key]: event.target.value,
										}))
									}
									type="text"
									value={address[key] ?? ''}
								/>
							</ClayForm.Group>
						))}
					</div>
				</DetailedCard>

				<div className="account-details-side">
					<DetailedCard
						cardIconAltText={i18n.translate('security')}
						cardTitle={i18n.translate('security')}
						clayIcon="lock"
					>
						<div className="mt-3">
							{CUSTOM_FIELDS.filter(
								({name: fieldName}) => fieldName !== 'Industry'
							).map(({label, name: fieldName}) => (
								<ClayForm.Group key={fieldName}>
									<label htmlFor={`account-${fieldName}`}>
										{i18n.translate(label)}
									</label>

									<ClayInput
										id={`account-${fieldName}`}
										onChange={(event) =>
											setCustomValues((previous) => ({
												...previous,
												[fieldName]: event.target.value,
											}))
										}
										type="text"
										value={customValues[fieldName] ?? ''}
									/>
								</ClayForm.Group>
							))}
						</div>
					</DetailedCard>
				</div>
			</div>
		</Page>
	);
}
