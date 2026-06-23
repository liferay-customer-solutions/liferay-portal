/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayForm, {ClayInput} from '@clayui/form';
import {ReactNode, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import Button from '~/components/Button/Button';
import {DetailedCard} from '~/components/DetailedCard/DetailedCard';
import Page from '~/components/Page/Page';
import usePublisherCatalog from '~/hooks/usePublisherCatalog';
import usePublisherDetails from '~/hooks/usePublisherDetails';
import i18n, {Word} from '~/i18n';
import PublisherDetailsService from '~/services/objects/PublisherDetails';

import type {PublisherDetailsEntry} from '~/types/publisher';

type EditableField = {
	component?: 'input' | 'textarea';
	key: keyof PublisherDetailsEntry;
	label: Word;
	type?: string;
};

const PUBLIC_FIELDS: EditableField[] = [
	{key: 'publisherName', label: 'publisher-name'},
	{component: 'textarea', key: 'description', label: 'company-description'},
	{key: 'email', label: 'company-email', type: 'email'},
	{key: 'supportEmail', label: 'support-email', type: 'email'},
	{key: 'salesEmail', label: 'sales-email', type: 'email'},
	{key: 'websiteURL', label: 'website'},
	{key: 'location', label: 'company-address'},
];

const PRIVATE_FIELDS: EditableField[] = [
	{key: 'fullName', label: 'full-name'},
	{key: 'role', label: 'role'},
	{key: 'privateEmail', label: 'email', type: 'email'},
	{key: 'phone', label: 'phone'},
	{key: 'paypalAccount', label: 'paypal-account'},
];

const ALL_FIELDS = [...PUBLIC_FIELDS, ...PRIVATE_FIELDS];

export default function PublisherProfileEdit() {
	const navigate = useNavigate();

	const {data: catalog, isLoading: isLoadingCatalog} = usePublisherCatalog();

	const catalogId = catalog?.id;

	const {
		isLoading: isLoadingDetails,
		mutate,
		publisherDetails,
	} = usePublisherDetails(catalogId);

	const [form, setForm] = useState<Partial<PublisherDetailsEntry>>({});
	const [error, setError] = useState<string>();
	const [saving, setSaving] = useState(false);

	useEffect(() => {
		if (publisherDetails) {
			setForm(publisherDetails);
		}
	}, [publisherDetails]);

	const onChange = (key: keyof PublisherDetailsEntry, fieldValue: string) =>
		setForm((previous) => ({...previous, [key]: fieldValue}));

	const onSave = async () => {
		if (!publisherDetails?.id) {
			setError(i18n.translate('no-publisher-profile-to-update'));

			return;
		}

		setError(undefined);
		setSaving(true);

		try {
			const body = ALL_FIELDS.reduce<Partial<PublisherDetailsEntry>>(
				(accumulator, {key}) => {
					(accumulator as Record<string, unknown>)[key] = form[key];

					return accumulator;
				},
				{}
			);

			await PublisherDetailsService.patchPublisherDetails(
				publisherDetails.id,
				body
			);

			await mutate();

			navigate('..');
		}
		catch (requestError) {
			setError(
				(requestError as Error)?.message ??
					i18n.translate('unable-to-update-publisher-profile')
			);
		}
		finally {
			setSaving(false);
		}
	};

	const renderField = ({component, key, label, type}: EditableField) => (
		<ClayForm.Group key={key}>
			<label htmlFor={`publisher-${key}`}>{i18n.translate(label)}</label>

			<ClayInput
				component={component === 'textarea' ? 'textarea' : 'input'}
				id={`publisher-${key}`}
				onChange={(event) => onChange(key, event.target.value)}
				type={type ?? 'text'}
				value={(form[key] as string) ?? ''}
			/>
		</ClayForm.Group>
	);

	const card = (
		title: Word,
		clayIcon: string,
		fields: EditableField[]
	): ReactNode => (
		<DetailedCard
			cardIconAltText={i18n.translate(title)}
			cardTitle={i18n.translate(title)}
			clayIcon={clayIcon}
		>
			<div className="mt-3">{fields.map(renderField)}</div>
		</DetailedCard>
	);

	return (
		<Page
			description={i18n.translate('manage-your-data-and-contacts')}
			pageRendererProps={{
				isLoading: isLoadingCatalog || isLoadingDetails,
			}}
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
			title={i18n.translate('edit-publisher-profile')}
		>
			{error && (
				<ClayAlert displayType="danger" title={i18n.translate('error')}>
					{error}
				</ClayAlert>
			)}

			<div
				className="mt-4"
				style={{
					display: 'grid',
					gap: 'var(--spacer-4)',
					gridTemplateColumns: 'repeat(auto-fit, minmax(20rem, 1fr))',
				}}
			>
				{card('public-information', 'globe', PUBLIC_FIELDS)}

				{card('private-information', 'password', PRIVATE_FIELDS)}
			</div>
		</Page>
	);
}
