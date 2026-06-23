/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useForm} from 'react-hook-form';
import {KeyedMutator} from 'swr';
import {z} from 'zod';
import i18n from '~/i18n';
import {ssaRoles} from '~/pages/Admin/SSADashboard/utils/constants';
import getFilteredItems from '~/pages/Admin/SSADashboard/utils/getFilteredItems';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {Liferay} from '~/services/liferay/liferay';

import MultiSelect from './MultiSelect/MultiSelect';

import type {UserAccount} from '~/types/accounts';
import type {APIResponse} from '~/types/api';

const formSchema = z.object({
	roles: z.array(z.object({value: z.string()})),
});

type FormValues = z.infer<typeof formSchema>;

type ManageUserModalProps = {
	accountERC: string;
	mutate: KeyedMutator<unknown>;
	onClose: () => void;
	user: UserAccount;
};

const ManageUserRolesModal = ({
	accountERC,
	mutate,
	onClose,
	user,
}: ManageUserModalProps) => {
	const ssaAccount = user.accountBriefs.find(
		(account) => account.externalReferenceCode === accountERC
	);

	const currentRoles =
		ssaAccount?.roleBriefs
			.filter((role) =>
				ssaRoles.some((ssaRole) => ssaRole.key === role.name)
			)
			.map((role) => ({
				key: role.name,
				label: role.name,
				value: role.name,
			})) || [];

	const {formState, handleSubmit, setValue, watch} = useForm<FormValues>({
		defaultValues: {roles: currentRoles},
	});

	const selectedRoles = watch('roles');

	const onSubmit = async (formData: FormValues) => {
		try {
			if (!ssaAccount?.id) {
				return Liferay.Util.openToast({
					message: i18n.translate('could-not-find-ssa-account'),
					type: 'danger',
				});
			}

			const {items: roles} =
				await HeadlessAdminUser.getAccountRoles(accountERC);

			const currentRolesSet = new Set(
				currentRoles.map((role) => role.value)
			);
			const newRolesSet = new Set(
				formData.roles.map((role) => role.value)
			);

			const rolesToAdd = roles.filter(
				(role) =>
					!currentRolesSet.has(role.name) &&
					newRolesSet.has(role.name)
			);

			const rolesToRemove = roles.filter(
				(role) =>
					currentRolesSet.has(role.name) &&
					!newRolesSet.has(role.name)
			);

			await Promise.all([
				...rolesToRemove.map((role) =>
					HeadlessAdminUser.deleteRoleAccountUser(
						ssaAccount?.id,
						role.id,
						user.id
					)
				),
				...rolesToAdd.map((role) =>
					HeadlessAdminUser.sendRoleAccountUser(
						ssaAccount?.id,
						role.id,
						user.id
					)
				),
			]);

			const updatedRoleBriefs = roles.filter((role) =>
				newRolesSet.has(role.name)
			);

			mutate(
				(users?: APIResponse<UserAccount>) => {
					return {
						...users,
						items: users?.items.map((prevUser) => {
							if (prevUser.id !== user.id) {
								return prevUser;
							}

							return {
								...prevUser,
								accountBriefs: prevUser.accountBriefs.map(
									(account) => {
										if (
											account.externalReferenceCode !==
											accountERC
										) {
											return account;
										}

										return {
											...account,
											roleBriefs:
												updatedRoleBriefs.reverse(),
										};
									}
								),
							};
						}),
					};
				},
				{revalidate: false}
			);
		}
		catch {
			return Liferay.Util.openToast({
				message: i18n.translate('unable-to-assign-roles'),
				title: i18n.translate('error'),
				type: 'danger',
			});
		}

		Liferay.Util.openToast({
			message: i18n.translate('user-roles-successfully-updated'),
			title: i18n.translate('success'),
		});

		onClose();
	};

	return (
		<form id="manage-roles" onSubmit={handleSubmit(onSubmit)}>
			<p>
				{i18n.translate(
					'manage-the-roles-associated-with-this-user-roles-determine-what-features-permissions-and-areas-of-the-platform-the-user-can-access-so-updating-them-allows-you-to-control-their-level-of-access-and-responsibilities'
				)}
			</p>

			<MultiSelect
				disabledClearAll
				errorMessage={formState.errors.roles?.message}
				inputName={i18n.translate('roles')}
				multiselectKey={`roles-${selectedRoles.length}`}
				onItemsChange={(roles) => {
					setValue('roles', roles as {value: string}[]);
				}}
				selectedItems={selectedRoles}
				sourceItems={getFilteredItems(selectedRoles, ssaRoles)}
			/>
		</form>
	);
};

export default ManageUserRolesModal;
