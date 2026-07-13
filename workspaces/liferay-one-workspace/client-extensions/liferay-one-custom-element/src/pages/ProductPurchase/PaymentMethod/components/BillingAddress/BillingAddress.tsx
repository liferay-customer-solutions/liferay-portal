/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';
import RadioCard from '~/components/RadioCard/RadioCard';
import Section from '~/components/Section/Section';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import useAccountAddresses from '~/pages/ProductPurchase/hooks/useAccountAddresses';
import useCommerceRegions from '~/pages/ProductPurchase/hooks/useCommerceRegions';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';

import BillingAddressForm from '../BillingAddressForm/BillingAddressForm';
import getPostalAddressDescription from './utils/getPostalAddressDescription';

import type {BillingAddress as BillingAddressType} from '~/types/orders';

const mapPostalAddressToBillingAddress = (
	postalAddress?: BillingAddressType
): BillingAddressType => ({
	city: postalAddress?.city || '',
	country: postalAddress?.countryISOCode || '',
	countryISOCode: postalAddress?.countryISOCode || 'US',
	name: postalAddress?.name || '',
	phoneNumber: postalAddress?.phoneNumber || '',
	regionISOCode: postalAddress?.regionISOCode || '',
	street1: postalAddress?.street1 || '',
	street2: postalAddress?.street2 || '',
	zip: postalAddress?.zip || '',
});

type BillingAddressProps = {
	hideNewAddressButton?: boolean;
	sectionName?: string;
};

const BillingAddress = ({
	hideNewAddressButton = false,
	sectionName = i18n.translate('billing-address'),
}: BillingAddressProps) => {
	const {payment, selectedAccount, setPayment} =
		useProductPurchaseLayoutContext();

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

	const setBillingAddress = (billingAddress: BillingAddressType) =>
		setPayment((previousPayment) => ({
			...previousPayment,
			billingAddress,
		}));

	useEffect(() => {
		if (
			hideNewAddressButton &&
			!!addresses.length &&
			!payment.billingAddress?.name
		) {
			const address = addresses[0];
			const newBillingAddress = mapPostalAddressToBillingAddress(address);

			setSelectedAddress(address.name || '');

			setBillingAddress(newBillingAddress);
		}
	}, [addresses, payment.billingAddress?.name, hideNewAddressButton]);

	const onSelectAddress = (address: BillingAddressType) => {
		setSelectedAddress(address.name || '');
		setShowNewAddressForm(false);

		const newBillingAddress = mapPostalAddressToBillingAddress(address);

		setBillingAddress(newBillingAddress);
	};

	const saveAddress = async (billingAddress: BillingAddressType) => {
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
		<Section label={sectionName} required>
			{addresses.map((address, index) => {
				const {description, title} =
					getPostalAddressDescription(address);

				return (
					<RadioCard
						className="mb-3"
						description={description}
						key={index}
						onChange={() => onSelectAddress(address)}
						selected={selectedAddress === address.name}
						title={title}
					/>
				);
			})}

			{!hideNewAddressButton && (
				<BillingAddressForm
					saveAddress={saveAddress}
					setBillingAddress={setBillingAddress}
					setSelectedAddress={setSelectedAddress}
					setShowNewAddressForm={setShowNewAddressForm}
					showNewAddressForm={showNewAddressForm}
				/>
			)}
		</Section>
	);
};

export default BillingAddress;
