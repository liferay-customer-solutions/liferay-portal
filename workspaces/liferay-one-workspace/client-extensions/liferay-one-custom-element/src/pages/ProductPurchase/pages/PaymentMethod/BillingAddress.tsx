/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';

import RadioCard from '../../../../components/RadioCard/RadioCard';
import Section from '../../../../components/Section/Section';
import i18n from '../../../../i18n';
import HeadlessAdminUser from '../../../../services/rest/HeadlessAdminUser';
import {useProductPurchaseOutletContext} from '../../ProductPurchaseOutlet';
import useAccountAddresses from '../../hooks/useAccountAddresses';
import useCommerceRegions from '../../hooks/useCommerceRegions';
import BillingAddressForm from './BillingAddressForm';
import getPostalAddressDescription from './getPostalAddressDescription';

const BillingAddress = () => {
	const {payment, selectedAccount, setPayment} =
		useProductPurchaseOutletContext();

	const {data: addressesResponse, mutate} = useAccountAddresses(
		selectedAccount?.id
	);
	const {data: countriesResponse} = useCommerceRegions();

	const [selectedAddress, setSelectedAddress] = useState(
		payment.billingAddress?.name || ''
	);
	const [showNewAddressForm, setShowNewAddressForm] = useState(false);

	const addresses = addressesResponse?.items ?? [];
	const countries = countriesResponse?.items ?? [];

	const setBillingAddress = (billingAddress: BillingAddress) =>
		setPayment((previousPayment) => ({
			...previousPayment,
			billingAddress,
		}));

	const onSelectAddress = (address: AccountPostalAddresses) => {
		setSelectedAddress(address.name);
		setShowNewAddressForm(false);

		const country = countries.find(
			(commerceCountry) =>
				(commerceCountry.title_i18n?.en_US || commerceCountry.name) ===
				address.addressCountry
		);

		const region = country?.regions.find(
			(commerceRegion) => commerceRegion.name === address.addressRegion
		);

		setBillingAddress({
			city: address.addressLocality || '',
			country: country?.a2 || address.addressCountry || '',
			countryISOCode: country?.a2 || '',
			name: address.name || '',
			phoneNumber: address.phoneNumber || '',
			regionISOCode: region?.regionCode || '',
			street1: address.streetAddressLine1 || '',
			street2: address.streetAddressLine2 || '',
			zip: address.postalCode ? String(address.postalCode) : '',
		});
	};

	const saveAddress = async (billingAddress: BillingAddress) => {
		const country = countries.find(
			(commerceCountry) => commerceCountry.a2 === billingAddress.country
		);

		const region = country?.regions.find(
			(commerceRegion) =>
				commerceRegion.regionCode === billingAddress.regionISOCode
		);

		await HeadlessAdminUser.postAddress(selectedAccount.id, {
			addressCountry: country?.title_i18n?.en_US || country?.name,
			addressLocality: billingAddress.city,
			addressRegion: region?.name,
			addressType: 'billing-and-shipping',
			name: billingAddress.name,
			phoneNumber: billingAddress.phoneNumber,
			postalCode: billingAddress.zip,
			primary: false,
			streetAddressLine1: billingAddress.street1,
			streetAddressLine2: billingAddress.street2,
		});

		await mutate();

		setSelectedAddress(billingAddress.name || '');
		setShowNewAddressForm(false);

		setBillingAddress(billingAddress);
	};

	return (
		<Section label={i18n.translate('billing-address')} required>
			{addresses.map((address) => {
				const {description, title} =
					getPostalAddressDescription(address);

				return (
					<RadioCard
						className="mb-3"
						description={description}
						key={address.id}
						onChange={() => onSelectAddress(address)}
						selected={selectedAddress === address.name}
						title={title}
					/>
				);
			})}

			<BillingAddressForm
				saveAddress={saveAddress}
				setBillingAddress={setBillingAddress}
				setSelectedAddress={setSelectedAddress}
				setShowNewAddressForm={setShowNewAddressForm}
				showNewAddressForm={showNewAddressForm}
			/>
		</Section>
	);
};

export default BillingAddress;
