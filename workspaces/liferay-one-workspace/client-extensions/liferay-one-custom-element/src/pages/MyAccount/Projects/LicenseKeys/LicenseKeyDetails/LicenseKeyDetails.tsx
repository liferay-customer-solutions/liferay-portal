/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ClayToggle} from '@clayui/form';
import {useParams} from 'react-router-dom';
import keyHorizontalSpritemap from '~/assets/icons/key_horizontal.svg';
import BackLink from '~/components/BackLink/BackLink';
import {
	ProjectActivationKey,
	useProjectActivationKeys,
} from '~/hooks/useProjectActivationKeys';
import {useProjectProducts} from '~/hooks/useProjectCommerce';
import {Word, translate} from '~/i18n';
import DetailsCard, {
	DetailsRow,
} from '~/pages/MyAccount/Projects/components/DetailsCard/DetailsCard';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import useLicenseKeyActions from '../hooks/useLicenseKeyActions';
import useLicenseKeySubscription from '../hooks/useLicenseKeySubscription';

export default function LicenseKeyDetails() {
	const {licenseKeyERC = '', projectId = ''} = useParams();

	const {activationKeys, loading, revalidate} = useProjectActivationKeys();
	const {products} = useProjectProducts(projectId);

	const {handleDeactivate, handleDownload, handleReactivate, handleRenew} =
		useLicenseKeyActions({
			products,
			projectExternalReferenceCode: projectId,
			revalidate,
		});

	const licenseKey = activationKeys.find((key) => key.id === licenseKeyERC);

	return (
		<div className="w-100">
			<BackLink path="..">{translate('license-keys')}</BackLink>

			{loading ? (
				<div className="p-4 text-neutral-7">{translate('loading')}</div>
			) : licenseKey ? (
				<LicenseKeyDetailsContent
					licenseKey={licenseKey}
					onDeactivate={() => handleDeactivate(licenseKey)}
					onDownload={() => handleDownload(licenseKey)}
					onReactivate={() => handleReactivate(licenseKey)}
					onRenew={() => handleRenew(licenseKey)}
				/>
			) : (
				<div className="p-4 text-neutral-7">
					{translate('no-results-found')}
				</div>
			)}
		</div>
	);
}

type LicenseKeyDetailsContentProps = {
	licenseKey: ProjectActivationKey;
	onDeactivate: () => void;
	onDownload: () => void;
	onReactivate: () => void;
	onRenew: () => void;
};

function LicenseKeyDetailsContent({
	licenseKey,
	onDeactivate,
	onDownload,
	onReactivate,
	onRenew,
}: LicenseKeyDetailsContentProps) {
	const {subscribed, toggleSubscription} = useLicenseKeySubscription(
		licenseKey.licenseKeyId
	);

	const detailsRows: DetailsRow[] = [
		{label: translate('environment-name'), value: licenseKey.name},
		{
			label: translate('description'),
			value: licenseKey.description || '-',
		},
		{label: translate('key-type'), value: licenseKey.keyType || '-'},
		{label: translate('host-name'), value: licenseKey.hostName || '-'},
		{
			label: translate('cluster-size'),
			value: licenseKey.clusterSize || '-',
		},
		{
			label: translate('environment-type'),
			value: translate(licenseKey.environmentType),
		},
		{label: translate('domains'), value: licenseKey.domain || '-'},
		{label: translate('start-date'), value: licenseKey.startDate || '-'},
		{
			label: translate('expiration-date'),
			value: licenseKey.expirationDate || '-',
		},
		{
			label: translate('status'),
			value: (
				<span className="align-items-center d-flex">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor: getStatusColor(licenseKey.status),
						}}
					/>

					{translate(licenseKey.status as Word)}
				</span>
			),
		},
	];

	if (licenseKey.products.length) {
		detailsRows.push({
			label: translate('products'),
			value: (
				<span className="d-flex flex-column">
					{licenseKey.products.map((product) => (
						<span key={product.externalReferenceCode}>
							{product.sizing
								? `${product.name} (${product.sizing})`
								: product.name}
						</span>
					))}
				</span>
			),
		});
	}

	return (
		<>
			<DetailsCard
				bodyClassName="mt-4"
				fullWidth
				headerActions={
					<div className="d-flex" style={{gap: 'var(--spacer-3)'}}>
						<ClayButton
							disabled={!licenseKey.active}
							displayType="secondary"
							onClick={onDownload}
						>
							{translate('download')}
						</ClayButton>

						<ClayButton displayType="secondary" onClick={onRenew}>
							{translate('renew')}
						</ClayButton>

						{licenseKey.active ? (
							<ClayButton
								displayType="danger"
								onClick={onDeactivate}
							>
								{translate('deactivate')}
							</ClayButton>
						) : (
							<ClayButton onClick={onReactivate}>
								{translate('reactivate')}
							</ClayButton>
						)}
					</div>
				}
				icon="key-horizontal"
				iconPosition="right"
				iconSpritemap={keyHorizontalSpritemap}
				rows={detailsRows}
				title="license-key-details"
			/>

			<div className="detailed-card-container mt-3">
				<div className="align-items-center d-flex justify-content-between">
					<span
						style={{
							color: 'var(--color-neutral-10)',
							fontWeight: 600,
						}}
					>
						{translate('expiration-notifications')}
					</span>

					<ClayToggle
						onToggle={toggleSubscription}
						toggled={subscribed}
					/>
				</div>

				<p className="mb-0 mt-2 text-neutral-7">
					{translate(
						'enable-notifications-through-email-when-this-activation-key-is-about-to-expire-30-days-before-15-days-before-and-on-the-day-of-expiration-you-can-unsubscribe-at-any-time'
					)}
				</p>
			</div>
		</>
	);
}
