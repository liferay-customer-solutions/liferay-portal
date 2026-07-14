/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayCheckbox} from '@clayui/form';
import {zodResolver} from '@hookform/resolvers/zod';
import {Controller, useForm} from 'react-hook-form';
import {Navigate} from 'react-router-dom';
import {Input} from '~/components/Input/Input';
import Select from '~/components/Select/Select';
import i18n from '~/i18n';
import LicenseTermsCheckbox from '~/pages/ProductPurchase/components/LicenseTermsCheckbox/LicenseTermsCheckbox';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import ProductPurchaseShell from '~/pages/ProductPurchase/components/ProductPurchaseShell/ProductPurchaseShell';
import commerceSchemas from '~/schema/commerceSchemas';
import FetcherError from '~/services/fetcher/FetcherError';
import {Liferay} from '~/services/liferay/liferay';
import LicenseKeys from '~/services/spring-boot/LicenseKeys';

import type {ActivationKeyFormData} from '~/services/commerce/ProductPurchaseDXPFree';

const PURPOSE_OPTIONS = [
	{key: 'personal-learning-education', name: 'Personal Learning / Education'},
	{key: 'proof-of-concept', name: 'Proof of Concept (POC)'},
	{key: 'development-and-testing', name: 'Development & Testing'},
	{key: 'internal-side-project', name: 'Internal Side Project'},
	{key: 'small-business-production', name: 'Small Business Production Use'},
];

const ActivationKeyForm = () => {
	const {
		actions: {previousStep},
		handlePurchase,
		product,
		selectedAccount,
	} = useProductPurchaseLayoutContext();

	const {
		control,
		formState: {errors, isValid},
		handleSubmit,
		register,
		setError,
		setValue,
		watch,
	} = useForm<ActivationKeyFormData>({
		defaultValues: {
			businessEmailAddress: Liferay.ThemeDisplay.getUserEmailAddress(),
			companyName: '',
			country: '',
			domain: '',
			extension: '',
			fullName: '',
			jobTitle: '',
			notifyMeAboutProducts: false,
			phoneNumber: '',
			purpose: '',
			termsAndConditions: false,
			userAgreement: false,
		},
		mode: 'onChange',
		resolver: zodResolver(commerceSchemas.activationKey),
	});

	if (!selectedAccount?.id) {
		return <Navigate replace to="/" />;
	}

	const acceptedTerms =
		Boolean(watch('termsAndConditions')) && Boolean(watch('userAgreement'));

	const onSubmit = async (formFields: ActivationKeyFormData) => {
		const owner = formFields.businessEmailAddress;

		try {
			await LicenseKeys.licenseKeyTypeFreeDomainsCheck({
				domains: formFields.domain,
				owner,
			});
		}
		catch (error) {
			if (error instanceof FetcherError && error.status === 409) {
				setError('domain', {
					message: i18n.translate(
						'a-license-key-for-the-entered-domain-already-exists'
					),
				});

				return;
			}

			throw error;
		}

		await handlePurchase(formFields);
	};

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {
					onClick: () => previousStep(),
				},
				continueButtonProps: {
					children: i18n.translate('get-activation-key'),
					disabled: !isValid,
					onClick: () => handleSubmit(onSubmit)(),
				},
			}}
			title={i18n.translate('activation-key-creation')}
		>
			<Input
				{...register('fullName')}
				errorMessage={errors.fullName?.message}
				label={i18n.translate('full-name')}
				required
			/>

			<Input
				{...register('businessEmailAddress')}
				errorMessage={errors.businessEmailAddress?.message}
				label={i18n.translate('business-email-address')}
				required
			/>

			<Input
				{...register('companyName')}
				errorMessage={errors.companyName?.message}
				label={i18n.translate('company-name')}
			/>

			<Input
				{...register('jobTitle')}
				errorMessage={errors.jobTitle?.message}
				label={i18n.translate('job-title')}
			/>

			<Input
				{...register('country')}
				errorMessage={errors.country?.message}
				label={i18n.translate('country')}
				required
			/>

			<Input
				{...register('phoneNumber')}
				errorMessage={errors.phoneNumber?.message}
				label={i18n.translate('phone-number')}
			/>

			<Select
				defaultOptionLabel={i18n.translate('select-an-option')}
				errors={errors as {[key: string]: {message?: string}}}
				label={i18n.translate('purpose')}
				name="purpose"
				onChange={({target: {value}}) =>
					setValue('purpose', value, {shouldValidate: true})
				}
				options={PURPOSE_OPTIONS}
				required
				value={watch('purpose')}
			/>

			<Input
				{...register('domain')}
				errorMessage={errors.domain?.message}
				helpMessage={i18n.translate('input-one-domain-name-per-instance')}
				label={i18n.translate('domain')}
				required
			/>

			<Controller
				control={control}
				name="notifyMeAboutProducts"
				render={({field}) => (
					<ClayCheckbox
						checked={Boolean(field.value)}
						className="mt-3"
						label={i18n.translate(
							'notify-me-about-products-services-and-events'
						)}
						onChange={() => field.onChange(!field.value)}
					/>
				)}
			/>

			<LicenseTermsCheckbox
				checked={acceptedTerms}
				onChange={() => {
					const value = !acceptedTerms;

					setValue('termsAndConditions', value, {
						shouldValidate: true,
					});
					setValue('userAgreement', value, {shouldValidate: true});
				}}
				product={product}
			/>
		</ProductPurchaseShell>
	);
};

export default ActivationKeyForm;
