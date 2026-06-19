/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import {useForm} from 'react-hook-form';

import FormInput from '../../../../components/Input/formInput';
import Select from '../../../../components/Select/Select';
import i18n from '../../../../i18n';
import zodSchema, {zodResolver} from '../../../../schema/zod';
import useCommerceRegions from '../../hooks/useCommerceRegions';

const defaultBillingAddress: BillingAddress = {
	city: '',
	country: '',
	countryISOCode: '',
	name: '',
	phoneNumber: '',
	regionISOCode: '',
	street1: '',
	street2: '',
	zip: '',
};

type BillingAddressFormProps = {
	saveAddress: (billingAddress: BillingAddress) => void;
	setBillingAddress: (billingAddress: BillingAddress) => void;
	setSelectedAddress: (name: string) => void;
	setShowNewAddressForm: (show: boolean) => void;
	showNewAddressForm: boolean;
};

const BillingAddressForm = ({
	saveAddress,
	setBillingAddress,
	setSelectedAddress,
	setShowNewAddressForm,
	showNewAddressForm,
}: BillingAddressFormProps) => {
	const {
		formState: {errors, isValid},
		handleSubmit,
		register,
		reset,
		setValue,
		watch,
	} = useForm<BillingAddress>({
		defaultValues: defaultBillingAddress,
		mode: 'onChange',
		resolver: zodResolver(zodSchema.billingAddress),
	});

	const {data: countriesResponse} = useCommerceRegions();

	const countries = countriesResponse?.items ?? [];

	const {country, regionISOCode} = watch();

	const states =
		countries.find((commerceCountry) => commerceCountry.a2 === country)
			?.regions ?? [];

	const inputProps = {
		errors,
		register,
		required: true,
	};

	const onSubmit = (billingAddress: BillingAddress) => {
		saveAddress(billingAddress);

		reset();
	};

	if (!showNewAddressForm) {
		return (
			<button
				className="align-items-center border d-flex justify-content-center mt-2 p-3 product-purchase-new-address rounded text-primary w-100"
				onClick={() => {
					setShowNewAddressForm(true);

					setBillingAddress({
						...defaultBillingAddress,
						countryISOCode: countries[0]?.a2 ?? '',
					});
				}}
			>
				<ClayIcon className="mr-2" symbol="plus" />

				{i18n.translate('new-address')}
			</button>
		);
	}

	return (
		<div className="border mt-2 p-4 rounded">
			<div className="align-items-center d-flex justify-content-between mb-3">
				<strong>{i18n.translate('new-address')}</strong>

				<ClayButton
					displayType="secondary"
					onClick={() => {
						setShowNewAddressForm(false);
						setSelectedAddress('');

						setBillingAddress(defaultBillingAddress);
					}}
					size="sm"
				>
					{i18n.translate('cancel')}
				</ClayButton>
			</div>

			<FormInput
				{...inputProps}
				label={i18n.translate('full-name')}
				name="name"
			/>

			<FormInput
				{...inputProps}
				label={i18n.translate('address')}
				name="street1"
			/>

			<FormInput
				{...inputProps}
				label={i18n.translate('address')}
				name="street2"
				required={false}
			/>

			<Select
				boldLabel
				label={i18n.translate('country')}
				name="country"
				onChange={({target: {value}}) => {
					const countryStates =
						countries.find(
							(commerceCountry) => commerceCountry.a2 === value
						)?.regions ?? [];

					setValue('country', value);
					setValue('countryISOCode', value);
					setValue(
						'regionISOCode',
						countryStates[0]?.regionCode ?? ''
					);
				}}
				options={countries.map((country) => ({
					key: country.a2,
					name: country.title_i18n?.en_US || country.name,
				}))}
				required
				value={country}
			/>

			<Select
				boldLabel
				defaultOption={false}
				disabled={!states.length}
				label={i18n.translate('state')}
				name="regionISOCode"
				onChange={({target: {value}}) =>
					setValue('regionISOCode', value)
				}
				options={states.map((state) => ({
					key: state.regionCode,
					name: state.name,
				}))}
				required={!!states.length}
				value={regionISOCode}
			/>

			<FormInput
				{...inputProps}
				label={i18n.translate('city')}
				name="city"
			/>

			<FormInput
				{...inputProps}
				label={i18n.translate('zip-area-code')}
				name="zip"
			/>

			<FormInput
				{...inputProps}
				label={i18n.translate('phone')}
				name="phoneNumber"
			/>

			<div className="d-flex justify-content-end">
				<ClayButton
					disabled={!isValid}
					onClick={handleSubmit(onSubmit)}
				>
					{i18n.translate('save')}
				</ClayButton>
			</div>
		</div>
	);
};

export default BillingAddressForm;
