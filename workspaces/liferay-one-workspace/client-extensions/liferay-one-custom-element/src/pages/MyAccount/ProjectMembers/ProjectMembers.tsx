/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import ClayTable from '@clayui/table';
import {useState} from 'react';
import Page from '~/components/Page/Page';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {useOneContext} from '~/context/OneContextProvider';
import {useFetch} from '~/hooks/useFetch';
import i18n, {translate} from '~/i18n';
import {
	ACCOUNT_ADMINISTRATOR,
	PARTNER_ACCOUNT_ADMIN,
} from '~/pages/MyAccount/AccountMembers/accountRoles';
import {useProjectMemberActions} from '~/pages/MyAccount/ProjectMembers/hooks/useProjectMemberActions';
import {useProjectMembers} from '~/pages/MyAccount/ProjectMembers/hooks/useProjectMembers';
import {
	PROJECT_ADMIN_ERC,
	getProjectRoleLabel,
} from '~/pages/MyAccount/ProjectMembers/projectRoles';
import {useHasProject} from '~/pages/MyAccount/Projects/hooks/useHasProject';
import {Liferay} from '~/services/liferay/liferay';

import './ProjectMembers.css';

import type {ProjectMembersRow} from '~/pages/MyAccount/ProjectMembers/types';
import type {Account} from '~/types/accounts';

const VISIBLE_MEMBERS = 2;

export default function ProjectMembers() {
	const accountId = Liferay.CommerceContext?.account?.accountId;
	const currentUserId = Liferay.ThemeDisplay.getUserId();

	const {userAccountModel} = useOneContext();

	const {hasProject, loading: hasProjectLoading} = useHasProject();

	const {data: account} = useFetch<Account>(
		accountId ? `/o/headless-admin-user/v1.0/accounts/${accountId}` : null
	);

	const {accountMemberOptions, loading, mutate, rows} = useProjectMembers();

	const [expandedProjects, setExpandedProjects] = useState<Set<number>>(
		new Set()
	);

	const isAccountAdmin = Boolean(
		userAccountModel?.hasAccountRoleName(ACCOUNT_ADMINISTRATOR) ||
			userAccountModel?.hasAccountRoleName(PARTNER_ACCOUNT_ADMIN) ||
			userAccountModel?.isAdmin
	);

	const {openEditCloudContacts, openEditProjectPermissions} =
		useProjectMemberActions({
			accountExternalReferenceCode: account?.externalReferenceCode ?? '',
			accountId: accountId ?? '',
			accountMemberOptions,
			mutate,
		});

	const canManageProject = (project: ProjectMembersRow) =>
		isAccountAdmin ||
		project.members.some(
			(member) =>
				String(member.userId) === currentUserId &&
				member.roleExternalReferenceCode === PROJECT_ADMIN_ERC
		);

	const toggleExpanded = (projectId: number) => {
		setExpandedProjects((previous) => {
			const next = new Set(previous);

			if (next.has(projectId)) {
				next.delete(projectId);
			}
			else {
				next.add(projectId);
			}

			return next;
		});
	};

	if (!hasProjectLoading && !hasProject) {
		return (
			<Page
				description={i18n.translate(
					'manage-project-roles-and-cloud-contact-designations'
				)}
			>
				<RestrictedFeatureMessage />
			</Page>
		);
	}

	return (
		<Page
			description={i18n.translate(
				'manage-project-roles-and-cloud-contact-designations'
			)}
			pageRendererProps={{isLoading: loading || hasProjectLoading}}
		>
			<div className="mt-3 project-members-card">
				{rows.length ? (
					<ClayTable borderless className="project-members-table">
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									{translate('project')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('project-members')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('cloud-contacts')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell />
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{rows.map((project) => {
								const expanded = expandedProjects.has(
									project.id
								);

								const visibleMembers = expanded
									? project.members
									: project.members.slice(0, VISIBLE_MEMBERS);

								const additionalMembers =
									project.members.length - VISIBLE_MEMBERS;

								const cloudContacts = project.members.filter(
									(member) => member.designations.length
								);

								return (
									<ClayTable.Row key={project.id}>
										<ClayTable.Cell>
											<div className="font-weight-bold">
												{project.name}
											</div>

											<ClayLabel displayType="success">
												{translate('active')}
											</ClayLabel>
										</ClayTable.Cell>

										<ClayTable.Cell>
											{project.members.length ? (
												<>
													{visibleMembers.map(
														(member) => (
															<div
																key={
																	member.userId
																}
															>
																{member.name}

																<span className="text-neutral-7">
																	{' '}
																	&middot;{' '}
																	{getProjectRoleLabel(
																		member.roleExternalReferenceCode
																	)}
																</span>
															</div>
														)
													)}

													{additionalMembers > 0 && (
														<button
															className="btn-unstyled project-members-more"
															onClick={() =>
																toggleExpanded(
																	project.id
																)
															}
															type="button"
														>
															{expanded
																? translate(
																		'show-less'
																	)
																: i18n.sub(
																		'x-more',
																		String(
																			additionalMembers
																		)
																	)}
														</button>
													)}
												</>
											) : (
												<span className="text-neutral-7">
													{translate(
														'if-there-is-no-project-admin-on-this-project-to-add-or-manage-team-members-contact-your-account-admin'
													)}
												</span>
											)}

											{!!project.members.length &&
												!project.hasProjectAdmin && (
													<div className="mt-1 text-neutral-7">
														{translate(
															'if-there-is-no-project-admin-on-this-project-to-add-or-manage-team-members-contact-your-account-admin'
														)}
													</div>
												)}
										</ClayTable.Cell>

										<ClayTable.Cell>
											{cloudContacts.length
												? cloudContacts.map(
														(member) => (
															<div
																key={
																	member.userId
																}
															>
																{member.name}

																<span className="text-neutral-7">
																	{' '}
																	&middot;{' '}
																	{member.designations.join(
																		', '
																	)}
																</span>
															</div>
														)
													)
												: '—'}
										</ClayTable.Cell>

										<ClayTable.Cell>
											{canManageProject(project) && (
												<ClayDropDown
													trigger={
														<ClayButton
															aria-label={translate(
																'manage-project-options'
															)}
															borderless
															className="text-neutral-7"
															displayType="unstyled"
														>
															<ClayIcon symbol="ellipsis-v" />
														</ClayButton>
													}
												>
													<ClayDropDown.ItemList>
														<ClayDropDown.Item
															onClick={() =>
																openEditProjectPermissions(
																	project
																)
															}
														>
															{translate(
																'edit-project-permissions'
															)}
														</ClayDropDown.Item>

														<ClayDropDown.Item
															onClick={() =>
																openEditCloudContacts(
																	project
																)
															}
														>
															{translate(
																'edit-cloud-contacts'
															)}
														</ClayDropDown.Item>
													</ClayDropDown.ItemList>
												</ClayDropDown>
											)}
										</ClayTable.Cell>
									</ClayTable.Row>
								);
							})}
						</ClayTable.Body>
					</ClayTable>
				) : (
					<div className="p-4 text-neutral-7">
						{translate('no-projects-were-found')}
					</div>
				)}
			</div>
		</Page>
	);
}
