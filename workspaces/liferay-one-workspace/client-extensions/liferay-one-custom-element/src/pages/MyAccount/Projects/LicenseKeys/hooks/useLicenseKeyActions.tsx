/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useConfirmationModal} from '~/hooks/useConfirmationModal';
import useModalContext from '~/hooks/useModalContext';
import {ProjectActivationKey} from '~/hooks/useProjectActivationKeys';
import {ProjectProduct} from '~/hooks/useProjectCommerce';
import {translate} from '~/i18n';
import {isUnassignedProject} from '~/pages/MyAccount/Projects/projects';
import {Liferay} from '~/services/liferay/liferay';
import LicenseKeyObject from '~/services/objects/LicenseKeys';
import LicenseKeysService from '~/services/spring-boot/LicenseKeys';

import LicenseKeyAdd from '../LicenseKeyAdd/LicenseKeyAdd';

type UseLicenseKeyActionsProps = {
	products: ProjectProduct[];
	projectExternalReferenceCode?: string;
	revalidate: () => void;
};

export function useLicenseKeyActions({
	products,
	projectExternalReferenceCode,
	revalidate,
}: UseLicenseKeyActionsProps) {
	const {onClose, onOpenModal} = useModalContext();
	const {openModal} = useConfirmationModal();

	const accountId = Liferay.CommerceContext.account?.accountId ?? undefined;

	const licenseKeyProjectExternalReferenceCode =
		projectExternalReferenceCode &&
		!isUnassignedProject(projectExternalReferenceCode)
			? projectExternalReferenceCode
			: undefined;

	function handleDeactivate(row: ProjectActivationKey) {
		openModal({
			body: translate('deactivate-license-key-confirmation'),
			header: translate('deactivate-license-key'),
			onConfirm: async () => {
				await LicenseKeyObject.deactivateLicenseKey(row.id);

				await revalidate();
			},
			status: 'danger',
		});
	}

	async function handleDownload(row: ProjectActivationKey) {
		await LicenseKeysService.downloadLicenseKey(
			row.licenseKeyId,
			`${row.name}.xml`
		);
	}

	function handleReactivate(row: ProjectActivationKey) {
		openModal({
			body: translate('reactivate-license-key-confirmation'),
			header: translate('reactivate-license-key'),
			onConfirm: async () => {
				await LicenseKeyObject.reactivateLicenseKey(row.id);

				await revalidate();
			},
			status: 'info',
		});
	}

	function handleNewKey() {
		onOpenModal({
			body: (
				<LicenseKeyAdd
					accountId={accountId}
					onClose={onClose}
					onGenerated={revalidate}
					products={products}
					projectExternalReferenceCode={
						licenseKeyProjectExternalReferenceCode
					}
				/>
			),
			header: translate('new-license-key'),
			size: 'lg',
		});
	}

	function handleRenew(row: ProjectActivationKey) {
		onOpenModal({
			body: (
				<LicenseKeyAdd
					accountId={accountId}
					initialIncludedExternalReferenceCodes={row.products.map(
						(product) => product.externalReferenceCode
					)}
					initialSizing={Object.fromEntries(
						row.products.map((product) => [
							product.externalReferenceCode,
							product.sizing,
						])
					)}
					initialValues={{
						clusterSize: row.clusterSize,
						description: row.description,
						domains: row.domain,
						environmentName: row.name,
						expirationDate: row.expirationDateValue,
						hostName: row.hostName,
						keyType: row.licenseType,
						startDate: row.startDateValue,
					}}
					onClose={onClose}
					onGenerated={revalidate}
					products={products}
					projectExternalReferenceCode={
						licenseKeyProjectExternalReferenceCode
					}
				/>
			),
			header: translate('renew-license-key'),
			size: 'lg',
		});
	}

	return {
		handleDeactivate,
		handleDownload,
		handleNewKey,
		handleReactivate,
		handleRenew,
	};
}

export default useLicenseKeyActions;
