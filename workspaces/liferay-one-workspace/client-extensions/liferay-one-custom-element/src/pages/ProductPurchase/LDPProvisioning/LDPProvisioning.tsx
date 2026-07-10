/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm from '@clayui/form';
import ClayMultiSelect from '@clayui/multi-select';
import {zodResolver} from '@hookform/resolvers/zod';
import {Controller, useForm} from 'react-hook-form';
import {Navigate} from 'react-router-dom';
import {z} from 'zod';
import {Input} from '~/components/Input/Input';
import i18n from '~/i18n';
import LicenseTermsCheckbox from '~/pages/ProductPurchase/components/LicenseTermsCheckbox/LicenseTermsCheckbox';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import ProductPurchaseShell from '~/pages/ProductPurchase/components/ProductPurchaseShell/ProductPurchaseShell';
import adminSchemas from '~/schema/adminSchemas';
import {Liferay} from '~/services/liferay/liferay';

type FormFields = z.infer<typeof adminSchemas.ldpProvisioning>;

type MultiSelectItem = {
	label: string;
	value: string;
};

const toValues = (items: MultiSelectItem[]) =>
	items.map((item) => item.value.trim()).filter(Boolean);

const LDPProvisioning = () => {
	const {
		actions: {nextStep, previousStep},
		product,
		selectedAccount,
		setLDPSettings,
	} = useProductPurchaseLayoutContext();

	const {
		control,
		formState: {errors, isValid},
		handleSubmit,
		register,
		setValue,
	} = useForm<FormFields>({
		defaultValues: {
			_refAllowedEmailDomains: [],
			_refIncidentReportContacts: [],
			acceptTerms: false,
			allowedEmailDomains: [],
			dataCenterLocation: 'INTERNAL',
			friendlyWorkspaceURL: '',
			incidentReportContacts: [],
			workspaceName: '',
			workspaceOwnerEmail: Liferay.ThemeDisplay.getUserEmailAddress(),
		},
		mode: 'onChange',
		resolver: zodResolver(adminSchemas.ldpProvisioning),
	});

	if (!selectedAccount?.id) {
		return <Navigate replace to="/" />;
	}

	const onSubmit = (formFields: FormFields) => {
		setLDPSettings({
			allowedEmailDomains: formFields.allowedEmailDomains,
			dataCenterLocation: formFields.dataCenterLocation,
			friendlyWorkspaceURL: formFields.friendlyWorkspaceURL,
			incidentReportContacts: formFields.incidentReportContacts,
			workspaceName: formFields.workspaceName,
			workspaceOwnerEmail: formFields.workspaceOwnerEmail,
		});

		nextStep();
	};

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {
					onClick: () => previousStep(),
				},
				continueButtonProps: {
					disabled: !isValid,
					onClick: () => handleSubmit(onSubmit)(),
				},
			}}
			title={i18n.translate('provisioning-details')}
		>
			<Input
				{...register('workspaceName')}
				errorMessage={errors.workspaceName?.message}
				label={i18n.translate('workspace-name')}
				required
			/>

			<Input
				{...register('workspaceOwnerEmail')}
				errorMessage={errors.workspaceOwnerEmail?.message}
				label={i18n.translate('workspace-owner-email')}
				required
			/>

			<Input
				{...register('friendlyWorkspaceURL')}
				errorMessage={errors.friendlyWorkspaceURL?.message}
				label={i18n.translate('friendly-workspace-url')}
				prependGroupItemSymbol="/"
			/>

			<Input
				disabled
				label={i18n.translate('data-center-location')}
				name="dataCenterLocation"
				value="INTERNAL"
			/>

			<Controller
				control={control}
				name="_refIncidentReportContacts"
				render={({field}) => (
					<ClayForm.Group
						className={
							errors.incidentReportContacts ? 'has-error' : ''
						}
					>
						<label>
							{i18n.translate('incident-report-contacts')}
						</label>

						<ClayMultiSelect
							items={field.value as MultiSelectItem[]}
							onItemsChange={(items: MultiSelectItem[]) => {
								field.onChange(items);

								setValue(
									'incidentReportContacts',
									toValues(items),
									{shouldValidate: true}
								);
							}}
						/>
					</ClayForm.Group>
				)}
			/>

			<Controller
				control={control}
				name="_refAllowedEmailDomains"
				render={({field}) => (
					<ClayForm.Group
						className={
							errors.allowedEmailDomains ? 'has-error' : ''
						}
					>
						<label>{i18n.translate('allowed-email-domains')}</label>

						<ClayMultiSelect
							items={field.value as MultiSelectItem[]}
							onItemsChange={(items: MultiSelectItem[]) => {
								field.onChange(items);

								setValue(
									'allowedEmailDomains',
									toValues(items),
									{shouldValidate: true}
								);
							}}
						/>
					</ClayForm.Group>
				)}
			/>

			<Controller
				control={control}
				name="acceptTerms"
				render={({field}) => (
					<LicenseTermsCheckbox
						checked={field.value}
						onChange={() => field.onChange(!field.value)}
						product={product}
					/>
				)}
			/>
		</ProductPurchaseShell>
	);
};

export default LDPProvisioning;
