/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import ClayForm, {ClaySelect} from '@clayui/form';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {translate} from '~/i18n';
import {ITicket} from '~/pages/BusinessEvents/types';
import HeadlessAdminUser from '~/services/headless/HeadlessAdminUser';
import {getAccountTickets} from '~/services/spring-boot/Jira';

import type {AccountBrief} from '~/types/accounts';

const TicketAttachmentsAdd = () => {
	const navigate = useNavigate();

	const [accounts, setAccounts] = useState<AccountBrief[]>([]);
	const [accountKey, setAccountKey] = useState('');
	const [loadingAccounts, setLoadingAccounts] = useState(true);
	const [loadingTickets, setLoadingTickets] = useState(false);
	const [ticketId, setTicketId] = useState('');
	const [tickets, setTickets] = useState<ITicket[]>([]);

	useEffect(() => {
		HeadlessAdminUser.getMyUserAccount()
			.then((userAccount) => {
				const accountBriefs = userAccount?.accountBriefs ?? [];

				setAccounts(accountBriefs);
				setAccountKey(accountBriefs[0]?.externalReferenceCode ?? '');
			})
			.catch(() => setAccounts([]))
			.finally(() => setLoadingAccounts(false));
	}, []);

	useEffect(() => {
		if (!accountKey) {
			setTickets([]);

			return;
		}

		const controller = new AbortController();

		setLoadingTickets(true);
		setTicketId('');

		getAccountTickets(accountKey)
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
	}, [accountKey]);

	if (loadingAccounts) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!accounts.length) {
		return (
			<div className="py-4">
				<RestrictedFeatureMessage />
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
					'select-the-account-and-ticket-you-want-to-attach-a-file-to'
				)}
			</h6>

			<div className="mt-4" style={{maxWidth: '32rem'}}>
				{accounts.length > 1 && (
					<ClayForm.Group>
						<label htmlFor="newAttachmentAccount">
							{translate('account')}
						</label>

						<ClaySelect
							id="newAttachmentAccount"
							onChange={(event) =>
								setAccountKey(event.target.value)
							}
							value={accountKey}
						>
							{accounts.map((account) => (
								<ClaySelect.Option
									key={account.externalReferenceCode}
									label={account.name}
									value={account.externalReferenceCode}
								/>
							))}
						</ClaySelect>
					</ClayForm.Group>
				)}

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
