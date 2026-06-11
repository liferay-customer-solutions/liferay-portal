/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {useMemo, useState} from 'react';

import Button from '../../components/Button/Button';
import Page from '../../components/Page';
import {useFetch} from '../../hooks/useFetch';
import i18n, {sub, translate} from '../../i18n';
import {Liferay} from '../../liferay/liferay';

import './AccountMembers.css';

const ROLE_ADMINISTRATOR = 'Account Administrator';
const ROLE_REQUESTER = 'Account Requester';

const AVATAR_COLORS = [
	'#2e5aac',
	'#e1a325',
	'#cf2c4f',
	'#287d3c',
	'#7d4fc9',
	'#0a7bae',
];

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50];

type RoleKey = 'administrator' | 'requester' | 'user';

type AccountMemberRow = {
	email: string;
	id: number;
	image?: string;
	isCurrentUser: boolean;
	name: string;
	roleKey: RoleKey;
	status: number;
	supportSeat: boolean;
};

const ROLE_OPTIONS: {key: RoleKey; label: string}[] = [
	{key: 'administrator', label: translate('administrator')},
	{key: 'requester', label: translate('requester')},
	{key: 'user', label: translate('user')},
];

function getRoleKey(roleBriefs: RoleBrief[] = []): RoleKey {
	const roleNames = roleBriefs.map(({name}) => name);

	if (roleNames.includes(ROLE_ADMINISTRATOR)) {
		return 'administrator';
	}

	if (roleNames.includes(ROLE_REQUESTER)) {
		return 'requester';
	}

	return 'user';
}

function hasImage(image?: string) {
	return Boolean(image) && !image?.includes('img_id=0');
}

function UserAvatar({image, name}: {image?: string; name: string}) {
	if (hasImage(image)) {
		return <img alt="" className="account-members-avatar" src={image} />;
	}

	const initials = name
		.split(' ')
		.filter(Boolean)
		.slice(0, 2)
		.map((word) => word[0])
		.join('');

	const colorIndex =
		name.split('').reduce((total, char) => total + char.charCodeAt(0), 0) %
		AVATAR_COLORS.length;

	return (
		<span
			className="account-members-avatar"
			style={{backgroundColor: AVATAR_COLORS[colorIndex]}}
		>
			{initials}
		</span>
	);
}

export default function AccountMembers() {
	const accountId = Liferay.CommerceContext?.account?.accountId;
	const currentUserId = Liferay.ThemeDisplay.getUserId();

	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(PAGE_SIZE_OPTIONS[0]);
	const [filterActive, setFilterActive] = useState(false);
	const [selectedRoles, setSelectedRoles] = useState<RoleKey[]>([]);

	const {
		data: account,
		error: accountError,
		loading: accountLoading,
	} = useFetch<Account>(
		accountId ? `/o/headless-admin-user/v1.0/accounts/${accountId}` : null
	);

	const {data, error, loading} = useFetch<APIResponse<UserAccount>>(
		accountId
			? `/o/headless-admin-user/v1.0/accounts/${accountId}/user-accounts`
			: null,
		{params: {pageSize: 100, sort: 'givenName:asc'}}
	);

	const members = useMemo<AccountMemberRow[]>(() => {
		return (data?.items ?? []).map((userAccount) => {
			const roleKey = getRoleKey(userAccount.roleBriefs);

			return {
				email: userAccount.emailAddress,
				id: userAccount.id,
				image: userAccount.image,
				isCurrentUser: String(userAccount.id) === currentUserId,
				name: userAccount.name,
				roleKey,
				status: (userAccount as {status?: number}).status ?? 0,
				supportSeat:
					roleKey === 'administrator' || roleKey === 'requester',
			};
		});
	}, [currentUserId, data]);

	const usedSeats = members.filter(({supportSeat}) => supportSeat).length;

	const maxSeats = Number(
		(account as {maxRequestors?: number})?.maxRequestors
	);

	const filteredMembers = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return members.filter((member) => {
			if (
				selectedRoles.length &&
				!selectedRoles.includes(member.roleKey)
			) {
				return false;
			}

			if (
				search &&
				!member.name.toLowerCase().includes(search) &&
				!member.email.toLowerCase().includes(search)
			) {
				return false;
			}

			return true;
		});
	}, [keywords, members, selectedRoles]);

	const paginatedMembers = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredMembers.slice(start, start + pageSize);
	}, [filteredMembers, page, pageSize]);

	const toggleRole = (roleKey: RoleKey) => {
		setPage(1);

		setSelectedRoles((previous) =>
			previous.includes(roleKey)
				? previous.filter((value) => value !== roleKey)
				: [...previous, roleKey]
		);
	};

	const seatsLabel = Number.isFinite(maxSeats)
		? sub('x-of-x-available', [
				String(Math.max(maxSeats - usedSeats, 0)),
				String(maxSeats),
			])
		: sub('x-in-use', [String(usedSeats)]);

	return (
		<Page
			description={i18n.translate(
				'invite-manage-roles-designate-incident-contacts'
			)}
			pageRendererProps={{
				error: accountError || error,
				isLoading: accountLoading || loading,
			}}
			title={i18n.translate('account-members')}
		>
			<div className="account-members-card mt-3">
				<div className="account-members-toolbar align-items-center d-flex">
					<ClayDropDown
						active={filterActive}
						onActiveChange={setFilterActive}
						trigger={
							<Button
								appendIcon="caret-bottom"
								className="account-members-filter-button"
								displayType="secondary"
								prependIcon="filter"
							>
								{translate('filter')}
							</Button>
						}
					>
						<ClayDropDown.ItemList>
							{ROLE_OPTIONS.map(({key, label}) => (
								<ClayDropDown.Item
									key={key}
									onClick={() => toggleRole(key)}
								>
									<ClayCheckbox
										checked={selectedRoles.includes(key)}
										label={label}
										onChange={() => toggleRole(key)}
									/>
								</ClayDropDown.Item>
							))}
						</ClayDropDown.ItemList>
					</ClayDropDown>

					<ClayInput.Group className="account-members-search">
						<ClayInput.GroupItem>
							<ClayInput
								className="input-group-inset input-group-inset-after"
								onChange={(event) => {
									setPage(1);
									setKeywords(event.target.value);
								}}
								placeholder={translate('search')}
								type="text"
								value={keywords}
							/>

							<ClayInput.GroupInsetItem after tag="span">
								<ClayIcon
									className="text-neutral-7"
									symbol="search"
								/>
							</ClayInput.GroupInsetItem>
						</ClayInput.GroupItem>
					</ClayInput.Group>

					<div className="account-members-seats align-items-center d-flex">
						<ClayIcon
							className="account-members-seats-info mr-2"
							symbol="info-circle"
						/>

						<span className="font-weight-semi-bold mr-1">
							{translate('support-seats')}:
						</span>

						<span>{seatsLabel}</span>
					</div>
				</div>

				{paginatedMembers.length ? (
					<>
						<ClayTable borderless className="account-members-table">
							<ClayTable.Head>
								<ClayTable.Row>
									<ClayTable.Cell headingCell>
										{translate('name')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('email')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('role')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('status')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('support-seat')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell />
								</ClayTable.Row>
							</ClayTable.Head>

							<ClayTable.Body>
								{paginatedMembers.map((member) => (
									<ClayTable.Row key={member.id}>
										<ClayTable.Cell>
											<div className="align-items-center d-flex">
												<UserAvatar
													image={member.image}
													name={member.name}
												/>

												<span className="account-members-name ml-3">
													{member.isCurrentUser
														? sub('x-me', [
																member.name,
															])
														: member.name}
												</span>
											</div>
										</ClayTable.Cell>

										<ClayTable.Cell>
											{member.email}
										</ClayTable.Cell>

										<ClayTable.Cell>
											{translate(member.roleKey)}
										</ClayTable.Cell>

										<ClayTable.Cell>
											<span className="align-items-center d-flex">
												<span className="account-members-status-dot" />

												{translate('active')}
											</span>
										</ClayTable.Cell>

										<ClayTable.Cell>
											{member.supportSeat ? (
												<ClayIcon
													className="account-members-seat-check"
													symbol="check-circle-full"
												/>
											) : (
												<span className="account-members-seat-empty">
													-
												</span>
											)}
										</ClayTable.Cell>

										<ClayTable.Cell>
											<ClayButton
												aria-label={translate(
													'manage-user-options'
												)}
												borderless
												className="text-neutral-7"
												displayType="unstyled"
											>
												<ClayIcon symbol="ellipsis-v" />
											</ClayButton>
										</ClayTable.Cell>
									</ClayTable.Row>
								))}
							</ClayTable.Body>
						</ClayTable>

						<div className="account-members-pagination">
							<ClayPaginationBarWithBasicItems
								activeDelta={pageSize}
								activePage={page}
								deltas={PAGE_SIZE_OPTIONS.map((label) => ({
									label,
								}))}
								labels={{
									paginationResults: translate(
										'showing-x-to-x-of-x'
									),
									perPageItems: translate('x-items'),
									selectPerPageItems: translate('x-items'),
								}}
								onDeltaChange={(delta) => {
									setPage(1);
									setPageSize(delta);
								}}
								onPageChange={setPage}
								totalItems={filteredMembers.length}
							/>
						</div>
					</>
				) : (
					<div className="p-4 text-neutral-7">
						{translate('no-account-members-were-found')}
					</div>
				)}
			</div>

			<div className="account-members-products-card mt-4">
				<h3 className="font-weight-bold text-neutral-10">
					{translate('manage-product-users')}
				</h3>

				<p className="text-neutral-7">
					{translate(
						'manage-roles-and-permissions-of-users-within-each-product'
					)}
				</p>

				<div className="d-flex flex-wrap mt-3">
					<Button
						appendIcon="shortcut"
						className="mb-2 mr-3"
						displayType="secondary"
					>
						{translate('manage-lxc-sm-users')}
					</Button>

					<Button
						appendIcon="shortcut"
						className="mb-2"
						displayType="secondary"
					>
						{translate('manage-analytics-cloud-users')}
					</Button>
				</div>
			</div>
		</Page>
	);
}
