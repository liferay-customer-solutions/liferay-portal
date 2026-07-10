/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import ClayForm, {ClaySelect} from '@clayui/form';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import ProjectSelector from '~/components/ProjectSelector/ProjectSelector';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {translate} from '~/i18n';
import {
	useSelectedProject,
	useUserProjects,
} from '~/pages/MyAccount/Projects/projects';
import {getProjectTickets} from '~/services/spring-boot/Jira';
import {ITicket} from '~/types/ticket';

const TicketAttachmentsAdd = () => {
	const navigate = useNavigate();

	const {
		hasAccountProjects,
		loading: projectsLoading,
		projects,
	} = useUserProjects();

	const {projectERC, selectProject} = useSelectedProject(
		projectsLoading,
		projects
	);

	const [loadingTickets, setLoadingTickets] = useState(false);
	const [ticketId, setTicketId] = useState('');
	const [tickets, setTickets] = useState<ITicket[]>([]);

	useEffect(() => {
		if (!projectERC) {
			setTickets([]);

			return;
		}

		const controller = new AbortController();

		setLoadingTickets(true);
		setTicketId('');

		getProjectTickets(projectERC)
			.then((response) => {
				if (!controller.signal.aborted) {
					setTickets((response.items as ITicket[]) ?? []);
				}
			})
			.catch(() => setTickets([]))
			.finally(() => {
				if (!controller.signal.aborted) {
					setLoadingTickets(false);
				}
			});

		return () => controller.abort();
	}, [projectERC]);

	if (projectsLoading) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!projects.length) {
		return (
			<div className="py-4">
				<RestrictedFeatureMessage
					message={
						hasAccountProjects
							? translate(
									'login-as-a-user-that-has-access-to-a-project-or-contact-your-project-administrator-to-add-you-to-a-project.'
								)
							: undefined
					}
				/>
			</div>
		);
	}

	return (
		<div className="py-4">
			<h1 className="font-weight-bold text-neutral-10">
				{translate('new-attachment')}
			</h1>

			<h6 className="font-weight-normal text-neutral-7">
				{translate(
					'select-the-project-and-ticket-you-want-to-attach-a-file-to'
				)}
			</h6>

			<div className="mt-4" style={{maxWidth: '32rem'}}>
				<ClayForm.Group>
					<ProjectSelector
						onSelect={selectProject}
						projects={projects}
						selectedProjectERC={projectERC}
					/>
				</ClayForm.Group>

				<ClayForm.Group>
					<label htmlFor="newAttachmentTicket">
						{translate('ticket')}
					</label>

					{loadingTickets ? (
						<ClayLoadingIndicator size="sm" />
					) : (
						<ClaySelect
							disabled={!tickets.length}
							id="newAttachmentTicket"
							onChange={(event) =>
								setTicketId(event.target.value)
							}
							value={ticketId}
						>
							<ClaySelect.Option
								label={translate('select-a-ticket')}
								value=""
							/>

							{tickets.map((ticket) => (
								<ClaySelect.Option
									key={ticket.ticketId}
									label={`${ticket.ticketId} — ${ticket.subject}`}
									value={ticket.ticketId}
								/>
							))}
						</ClaySelect>
					)}

					{!loadingTickets && !tickets.length && (
						<div className="mt-1 text-neutral-7">
							{translate('no-support-tickets-were-found')}
						</div>
					)}
				</ClayForm.Group>

				<div className="d-flex mt-4">
					<Button
						displayType="secondary"
						onClick={() => navigate('/')}
					>
						{translate('cancel')}
					</Button>

					<Button
						className="ml-3"
						disabled={!ticketId}
						displayType="primary"
						onClick={() => navigate(`/new/${ticketId}`)}
					>
						{translate('continue')}
					</Button>
				</div>
			</div>
		</div>
	);
};

export default TicketAttachmentsAdd;
