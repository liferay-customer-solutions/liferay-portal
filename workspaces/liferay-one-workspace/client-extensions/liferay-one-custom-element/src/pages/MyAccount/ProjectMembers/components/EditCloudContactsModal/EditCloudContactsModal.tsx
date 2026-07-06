/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useMemo, useState} from 'react';
import {useProjectProducts} from '~/hooks/useProjectCommerce';
import i18n, {translate} from '~/i18n';
import MultiSelect from '~/pages/Admin/SSADashboard/components/MultiSelect/MultiSelect';
import {getAvailableDesignations} from '~/pages/MyAccount/ProjectMembers/projectRoles';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';
import getProductOrderTypes from '~/utils/getProductOrderTypes';

import type {ProjectMembersRow} from '~/pages/MyAccount/ProjectMembers/types';

type DesignationItem = {label: string; value: string};

type EditCloudContactsModalProps = {
	accountExternalReferenceCode: string;
	mutate: () => Promise<unknown>;
	onClose: () => void;
	project: ProjectMembersRow;
};

const EditCloudContactsModal = ({
	accountExternalReferenceCode,
	mutate,
	onClose,
	project,
}: EditCloudContactsModalProps) => {
	const {products} = useProjectProducts(project.externalReferenceCode);

	const availableDesignations = useMemo(() => {
		const productTypeExternalReferenceCodes = products.flatMap((product) =>
			(product.specifications ?? [])
				.filter(
					(specification) =>
						specification.specificationKey === 'product-type'
				)
				.map(
					(specification) =>
						getProductOrderTypes(specification.value)
							.externalReferenceCode
				)
		);

		return getAvailableDesignations(productTypeExternalReferenceCodes);
	}, [products]);

	const [designationsByUserId, setDesignationsByUserId] = useState<
		Record<number, DesignationItem[]>
	>(() => {
		const initial: Record<number, DesignationItem[]> = {};

		project.members.forEach((member) => {
			initial[member.userId] = member.designations.map((designation) => ({
				label: designation,
				value: designation,
			}));
		});

		return initial;
	});

	const onSubmit = async (event: React.FormEvent) => {
		event.preventDefault();

		try {
			const {items: accountRoles} =
				await HeadlessAdminUser.getAccountRoles(
					accountExternalReferenceCode
				);

			const accountRoleByName = new Map(
				accountRoles.map((accountRole) => [
					accountRole.name,
					accountRole,
				])
			);

			const operations: Promise<unknown>[] = [];

			project.members.forEach((member) => {
				const currentDesignations = new Set(member.designations);
				const selectedDesignations = new Set(
					(designationsByUserId[member.userId] ?? []).map(
						(designation) => designation.value
					)
				);

				availableDesignations.forEach((designation) => {
					const accountRole = accountRoleByName.get(designation);

					if (!accountRole) {
						return;
					}

					if (
						selectedDesignations.has(designation) &&
						!currentDesignations.has(designation)
					) {
						operations.push(
							HeadlessAdminUser.sendRoleAccountUser(
								accountRole.accountId,
								accountRole.id,
								member.userId
							)
						);
					}
					else if (
						!selectedDesignations.has(designation) &&
						currentDesignations.has(designation)
					) {
						operations.push(
							HeadlessAdminUser.deleteRoleAccountUser(
								accountRole.accountId,
								accountRole.id,
								member.userId
							)
						);
					}
				});
			});

			await Promise.all(operations);

			await mutate();

			Liferay.Util.openToast({
				message: translate('cloud-contacts-successfully-updated'),
				title: translate('success'),
			});

			onClose();
		}
		catch {
			Liferay.Util.openToast({
				message: translate('unable-to-update-cloud-contacts'),
				title: translate('error'),
				type: 'danger',
			});
		}
	};

	const sourceItems = (userId: number): DesignationItem[] => {
		const selected = designationsByUserId[userId] ?? [];

		return availableDesignations
			.filter(
				(designation) =>
					!selected.some((item) => item.value === designation)
			)
			.map((designation) => ({label: designation, value: designation}));
	};

	return (
		<form id="edit-cloud-contacts" onSubmit={onSubmit}>
			<p>
				{i18n.sub(
					'assign-cloud-contact-designations-for-x',
					project.name
				)}
			</p>

			{project.members.map((member) => (
				<div className="mb-3" key={member.userId}>
					<label>{member.name}</label>

					<MultiSelect
						inputName={translate('cloud-contacts')}
						multiselectKey={`designations-${member.userId}-${
							(designationsByUserId[member.userId] ?? []).length
						}`}
						onItemsChange={(designations) =>
							setDesignationsByUserId((previous) => ({
								...previous,
								[member.userId]:
									designations as DesignationItem[],
							}))
						}
						selectedItems={
							designationsByUserId[member.userId] ?? []
						}
						sourceItems={sourceItems(member.userId)}
					/>
				</div>
			))}
		</form>
	);
};

export default EditCloudContactsModal;
