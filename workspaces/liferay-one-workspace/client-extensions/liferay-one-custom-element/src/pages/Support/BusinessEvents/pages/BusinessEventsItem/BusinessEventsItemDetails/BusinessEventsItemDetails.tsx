/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Nav} from '@clayui/core';
import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useModal} from '@clayui/modal';
import NavigationBar from '@clayui/navigation-bar';
import {useCallback, useEffect, useState} from 'react';
import {Link, useLocation, useNavigate, useParams} from 'react-router-dom';

import {translate} from '../../../../../../i18n';
import {Liferay} from '../../../../../../liferay/liferay';
import AssociatedTicketsContainer from '../../../components/AssociatedTicketsContainer/AssociatedTicketsContainer';
import ButtonDropDown from '../../../../../../components/ButtonDropDown/ButtonDropDown';
import ManageEventModal from '../../../components/ManageEventModal/ManageEventModal';
import useAccountsTickets from '../../../hooks/useAccountsTickets';
import useCanViewTickets from '../../../hooks/useCanViewTickets';
import useGetBusinessEvent from '../../../hooks/useGetBusinessEvent';
import useHasAllEventsPermissions from '../../../hooks/useHasAllEventsPermissions';
import {ITicket} from '../../../types';
import {getFormattedDate} from '../../../utils/getFormattedDate';
import {getFormattedTime} from '../../../utils/getFormattedTime';
import getKebabCase from '../../../../../../utils/getKebabCase';
import parseAssociatedTickets from '../../../utils/parseAssociatedTickets';

const BusinessEventsItemDetails = () => {
	const {accountKey, id} = useParams<{accountKey: string; id: string}>();

	const navigate = useNavigate();

	const {
		businessEvent,
		fetchBusinessEvent,
		loading: loadingBusinessEvents,
	} = useGetBusinessEvent(accountKey || '', id || '');

	const [modalType, setModalType] = useState('');
	const {hasAllEventsPermissions} = useHasAllEventsPermissions(
		accountKey || ''
	);

	const {loading: loadingTickets, tickets} = useAccountsTickets(
		businessEvent,
		accountKey,
		loadingBusinessEvents || !businessEvent?.associatedTickets
	);

	const {canViewTickets, loading: loadingJiraAccountChecking} =
		useCanViewTickets(accountKey || '');

	const loading = loadingBusinessEvents || loadingJiraAccountChecking;

	const location = useLocation();

	const {observer, onOpenChange, open} = useModal();

	const userOptions = [
		{
			customOptionStyle: 'pr-5',
			icon: <ClayIcon symbol="pencil" />,
			label: translate('edit-event'),
			onClick: () => {
				navigate(`/${accountKey}/business-events/${id}/edit`);
			},
		},
		{
			customOptionStyle: 'pr-5',
			icon: <ClayIcon symbol="check-circle" />,
			label: translate('record-actual-event-date'),
			onClick: () => {
				setModalType('goLiveEvent');
				onOpenChange(true);
			},
		},
		{
			customOptionStyle: 'cancel-event-option pr-5',
			icon: <ClayIcon symbol="trash" />,
			label: translate('cancel-event'),
			onClick: () => {
				setModalType('cancelEvent');
				onOpenChange(true);
			},
		},
	];

	const [ticketOptions, setTicketOptions] = useState<ITicket[]>([]);

	const handleOnCancel = useCallback(() => {
		fetchBusinessEvent();

		Liferay.Util.openToast({
			message: translate('business-event-canceled-successfully'),
			type: 'success',
		});
	}, [fetchBusinessEvent]);

	const handleOnCompleted = useCallback(() => {
		fetchBusinessEvent();

		Liferay.Util.openToast({
			message: translate(
				'business-event-actual-event-date-recorded-successfully'
			),
			type: 'success',
		});
	}, [fetchBusinessEvent]);

	const handleCloseModal = (isOpen: boolean) => {
		if (!isOpen) {
			navigate(`/${accountKey}/business-events/${id}`);
		}

		onOpenChange(isOpen);
	};

	useEffect(() => {
		if (businessEvent && tickets) {
			const associatedTickets = parseAssociatedTickets(
				businessEvent.associatedTickets
			);

			setTicketOptions([
				...(tickets?.filter((ticket) =>
					associatedTickets.includes(String(ticket.ticketId))
				) || []),
			]);
		}

		const params = new URLSearchParams(location.search);
		const modalTypeParam = params.get('openModal');

		if (modalTypeParam === 'goLiveEvent') {
			setModalType('goLiveEvent');

			onOpenChange(true);
		}
	}, [businessEvent, location.search, onOpenChange, tickets]);

	if (loading) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!businessEvent) {
		return <div>{translate('no-data-found')}</div>;
	}

	return (
		<div>
			<div className="be-breadcrumbs font-weight-semi-bold mb-4">
				<span className="mx-2">
					<Link to={`/${accountKey}/business-events/`}>
						<ClayIcon className="mr-1" symbol="order-arrow-left" />

						{translate('back-to-business-events')}
					</Link>
				</span>
			</div>

			<div>
				<div
					className={`align-items-center font-weight-semi-bold be-status be-status-${businessEvent?.eventStatus?.key?.toLowerCase()} mb-1 d-inline px-2 py-1`}
				>
					{translate(
						getKebabCase(
							businessEvent?.eventStatus?.key as string
						) as any
					)}
				</div>

				<div className="alight-items-center d-flex justify-content-between mb-4 mt-2">
					<div className="font-weight-bold text-neutral-10">
						<h3>{businessEvent.name}</h3>
					</div>

					{hasAllEventsPermissions &&
						!['Canceled', 'Completed'].includes(
							businessEvent.eventStatus?.key!
						) && (
							<div className="be-actions">
								<ButtonDropDown
									items={userOptions}
									label={translate('actions')}
									menuElementAttrs={{
										className: 'p-0',
									}}
								/>
							</div>
						)}
				</div>
			</div>

			<div className="mb-4">
				<NavigationBar
					fluidSize={false}
					triggerLabel={translate('event-details')}
				>
					<Nav.Item>
						<Nav.Link
							active={true}
							aria-label={`Switch to ${translate('event-details')}`}
							className="be-nav-link text-neutral-10"
						>
							{translate('event-details')}
						</Nav.Link>
					</Nav.Item>
					<Nav.Item
						onClick={() =>
							navigate(
								`/${accountKey}/business-events/${id}/activity-history`
							)
						}
					>
						<Nav.Link
							active={false}
							aria-label={`Switch to ${translate('activity-history')}`}
							className="be-nav-link text-neutral-10"
						>
							{translate('activity-history')}
						</Nav.Link>
					</Nav.Item>
				</NavigationBar>
			</div>

			<div className="mt-4">
				<div className="event-detail-container">
					{businessEvent?.eventType && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
								{translate('event-type')}
							</div>

							<div className="d-inline-block event-detail-value font-weight-semi-bold rounded text-neutral-9">
								{translate(
									getKebabCase(
										businessEvent?.eventType?.key as string
									) as any
								)}
							</div>
						</div>
					)}

					{businessEvent?.currentLiferayVersion?.key && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
								{translate('current-version')}
							</div>

							<div className="d-inline-block event-detail-value font-weight-semi-bold rounded text-neutral-9">
								{businessEvent?.currentLiferayVersion?.name}
							</div>
						</div>
					)}

					{businessEvent?.newLiferayVersion?.key && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
								{translate('new-version')}
							</div>

							<div className="d-inline-block event-detail-value font-weight-semi-bold rounded text-neutral-9">
								{businessEvent?.newLiferayVersion?.name}
							</div>
						</div>
					)}

					{businessEvent?.description && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-2 text-neutral-8">
								{translate('details')}
							</div>

							<div className="d-inline-block text-neutral-9">
								{businessEvent?.description}
							</div>
						</div>
					)}

					{businessEvent?.plannedEventDate && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
								{translate('planned-event-date')}
							</div>

							<div className="d-inline-block event-detail-value font-weight-semi-bold rounded text-neutral-9">
								<div className="text-neutral-10">
									{getFormattedDate(
										businessEvent?.plannedEventDate,
										'day2DMonthSYearN',
										'UTC'
									)}
								</div>

								<div className="be-subtitle text-neutral-7">
									{getFormattedTime(
										businessEvent?.plannedEventDate,
										'UTC'
									)}
								</div>
							</div>
						</div>
					)}

					{businessEvent?.actualEventDate && (
						<div className="event-detail-item mb-4">
							<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
								{translate('actual-event-date')}
							</div>

							<div className="d-inline-block event-detail-value font-weight-semi-bold rounded text-neutral-9">
								<div className="text-neutral-10">
									{getFormattedDate(
										businessEvent?.actualEventDate,
										'day2DMonthSYearN',
										'UTC'
									)}
								</div>

								<div className="be-subtitle text-neutral-7">
									{getFormattedTime(
										businessEvent?.actualEventDate,
										'UTC'
									)}
								</div>
							</div>
						</div>
					)}

					{!loadingTickets ? (
						!canViewTickets ? (
							<p
								dangerouslySetInnerHTML={{
									__html: translate(
										'we-apologize-for-the-inconvenience-but-we-ve-detected-a-system-error-with-this-project'
									),
								}}
							/>
						) : (
							Boolean(ticketOptions.length) && (
								<div className="event-detail-item mb-4">
									<div className="event-detail-title font-weight-semi-bold mb-1 text-neutral-8">
										{translate('associated-tickets')}
									</div>

									<div className="w-50">
										<AssociatedTicketsContainer
											ticketOptions={ticketOptions}
										/>
									</div>
								</div>
							)
						)
					) : (
						<div className="w-25">
							<ClayLoadingIndicator size="sm" />
						</div>
					)}
				</div>
			</div>

			{businessEvent && open && (
				<ManageEventModal
					accountExternalReferenceCode={accountKey || ''}
					businessEvent={businessEvent}
					closeFunction={handleCloseModal}
					modalType={modalType}
					observer={observer}
					onCancel={handleOnCancel}
					onCompleted={handleOnCompleted}
				/>
			)}
		</div>
	);
};

export default BusinessEventsItemDetails;
