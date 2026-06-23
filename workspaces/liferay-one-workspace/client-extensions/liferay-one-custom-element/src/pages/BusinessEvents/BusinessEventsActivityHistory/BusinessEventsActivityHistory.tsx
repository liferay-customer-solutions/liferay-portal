/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Nav} from '@clayui/core';
import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useModal} from '@clayui/modal';
import NavigationBar from '@clayui/navigation-bar';
import {useCallback, useMemo, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';
import Table, {
	IRow,
} from '~/components/BusinessEventsTable/BusinessEventsTable';
import {translate} from '~/i18n';
import ButtonDropDown from '~/pages/BusinessEvents/components/ButtonDropDown/ButtonDropDown';
import ManageEventModal from '~/pages/BusinessEvents/components/ManageEventModal/ManageEventModal';
import useGetBusinessEvent from '~/pages/BusinessEvents/hooks/useGetBusinessEvent';
import useGetBusinessEventVersions from '~/pages/BusinessEvents/hooks/useGetBusinessEventVersions';
import useHasAllEventsPermissions from '~/pages/BusinessEvents/hooks/useHasAllEventsPermissions';
import {getFormattedDate} from '~/pages/BusinessEvents/utils/getFormattedDate';
import {getFormattedTime} from '~/pages/BusinessEvents/utils/getFormattedTime';
import {Liferay} from '~/services/liferay/liferay';

const BusinessEventsActivityHistory = () => {
	const {accountKey, id} = useParams<{accountKey: string; id: string}>();

	const navigate = useNavigate();

	const {businessEvent, fetchBusinessEvent, loading} = useGetBusinessEvent(
		accountKey || '',
		id || ''
	);

	const {
		businessEventVersions,
		fetchBusinessEventVersions,
		loading: loadingVersions,
	} = useGetBusinessEventVersions(accountKey || '', id || '');

	const {hasAllEventsPermissions} = useHasAllEventsPermissions(
		accountKey || ''
	);

	const [modalType, setModalType] = useState('');

	const {observer, onOpenChange, open} = useModal();

	const rows = useMemo(() => {
		if (businessEventVersions?.length > 0) {
			return businessEventVersions.map((businessEventVersion) => {
				return {
					change: (
						<div className="font-weight-semi-bold text-neutral-10">
							{businessEventVersion?.change?.name}
						</div>
					),

					comment: (
						<div className="text-neutral-10">
							{businessEventVersion?.comment}
						</div>
					),
					date: (
						<div>
							<div className="text-neutral-10">
								{getFormattedDate(
									businessEventVersion?.createdDate,
									'day2DMonthSYearN',
									'UTC'
								)}
							</div>

							<div className="be-subtitle text-neutral-7">
								{getFormattedTime(
									businessEventVersion?.createdDate,
									'UTC'
								)}
							</div>
						</div>
					),
					user: (
						<div className="align-items-center d-flex">
							<div className="font-weight-semi-bold m-0 mr-1 text-neutral-10 text-truncate">
								{businessEventVersion?.author}
							</div>
						</div>
					),
				};
			});
		}

		return [];
	}, [businessEventVersions]);

	const handleOnCancel = useCallback(() => {
		fetchBusinessEvent();

		fetchBusinessEventVersions();

		Liferay.Util.openToast({
			message: translate('business-event-canceled-successfully'),
			type: 'success',
		});
	}, [fetchBusinessEvent, fetchBusinessEventVersions]);

	const handleOnCompleted = useCallback(() => {
		fetchBusinessEvent();

		fetchBusinessEventVersions();

		Liferay.Util.openToast({
			message: translate(
				'business-event-actual-event-date-recorded-successfully'
			),
			type: 'success',
		});
	}, [fetchBusinessEvent, fetchBusinessEventVersions]);

	if (loading || loadingVersions) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!businessEvent) {
		return <div>{translate('no-data-found')}</div>;
	}

	const columns = [
		{
			columnKey: 'change',
			label: translate('change'),
		},
		{
			columnKey: 'user',
			label: translate('user'),
		},
		{
			columnKey: 'comment',
			label: translate('comment'),
		},
		{
			columnKey: 'date',
			label: translate('date'),
		},
	];

	const userOptions = [
		{
			customOptionStyle: 'pr-5',
			icon: <ClayIcon symbol="pencil" />,
			label: translate('edit-event'),
			onClick: () => {
				navigate('../edit');
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

	return (
		<div>
			<div className="be-breadcrumbs font-weight-semi-bold mb-4">
				<span className="mx-2">
					<Link to="../..">
						<ClayIcon className="mr-1" symbol="order-arrow-left" />

						{translate('back-to-business-events')}
					</Link>
				</span>
			</div>

			<div>
				<div
					className={`align-items-center font-weight-semi-bold be-status be-status-${businessEvent?.eventStatus?.key.toLowerCase()} mb-1 d-inline px-2 py-1`}
				>
					{businessEvent?.eventStatus?.name}
				</div>

				<div className="align-items-center d-flex justify-content-between mb-4 mt-2">
					<div className="font-weight-bold text-neutral-10">
						<h3>{businessEvent.name}</h3>
					</div>

					{hasAllEventsPermissions &&
						!['Canceled', 'Completed'].includes(
							businessEvent.eventStatus?.key!
						) && (
							<div>
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
					triggerLabel={translate('activity-history')}
				>
					<Nav.Item onClick={() => navigate('..')}>
						<Nav.Link
							active={false}
							aria-label={`Switch to ${translate('event-details')}`}
							className="be-nav-link text-neutral-10"
						>
							{translate('event-details')}
						</Nav.Link>
					</Nav.Item>
					<Nav.Item>
						<Nav.Link
							active={true}
							aria-label={`Switch to ${translate('activity-history')}`}
							className="be-nav-link text-neutral-10"
						>
							{translate('activity-history')}
						</Nav.Link>
					</Nav.Item>
				</NavigationBar>
			</div>

			<div className="mt-4"></div>

			{businessEvent && open && (
				<ManageEventModal
					accountExternalReferenceCode={accountKey || ''}
					businessEvent={businessEvent}
					closeFunction={onOpenChange}
					modalType={modalType}
					observer={observer}
					onCancel={handleOnCancel}
					onCompleted={handleOnCompleted}
				/>
			)}

			<div className="">
				{businessEventVersions?.length ? (
					<div className="versions-table">
						<Table
							columns={columns}
							rows={rows as unknown as IRow[]}
						/>
					</div>
				) : (
					<div className="p-3">
						{translate('no-history-of-activity-was-found')}
					</div>
				)}
			</div>
		</div>
	);
};

export default BusinessEventsActivityHistory;
