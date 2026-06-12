/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Nav, useModal} from '@clayui/core';
import {ClayInput, ClayRadio} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import NavigationBar from '@clayui/navigation-bar';
import {FieldArray, Formik} from 'formik';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {Link, useNavigate, useParams} from 'react-router-dom';

import Button from '../../../../../../components/Button/Button';
import DatePicker from '../../../../../../components/DatePicker/DatePicker';
import TimePicker from '../../../../../../components/TimePicker/TimePicker';
import {translate} from '../../../../../../i18n';
import {Liferay} from '../../../../../../liferay/liferay';
import getKebabCase from '../../../../../../utils/getKebabCase';
import {isValidDate} from '../../../../../../utils/validations.form';
import AssociatedTicketsContainer from '../../../components/AssociatedTicketsContainer/AssociatedTicketsContainer';
import Input from '../../../components/Input/Input';
import Select, {IOption} from '../../../components/Select/Select';
import useAccountsTickets from '../../../hooks/useAccountsTickets';
import useCanViewTickets from '../../../hooks/useCanViewTickets';
import useGetBusinessEvent from '../../../hooks/useGetBusinessEvent';
import useGetBusinessEventTypesList from '../../../hooks/useGetBusinessEventTypesList';
import useGetLiferayVersions from '../../../hooks/useGetLiferayVersions';
import useGetUTCTimeZonesList from '../../../hooks/useGetUTCTimeZonesList';
import useHasAllEventsPermissions from '../../../hooks/useHasAllEventsPermissions';
import {updateBusinessEvent} from '../../../services/jira/Jira';
import {IBusinessEvent, ITicket} from '../../../types';
import {containsOption} from '../../../utils/containsOption';
import {
	getFormattedEventDateTime,
	normalizeEventDateTime,
} from '../../../utils/getFormattedEventDate';
import parseAssociatedTickets from '../../../utils/parseAssociatedTickets';
import useIsSaasOnly from '../../../utils/useIsSaasOnly';
import BusinessEventsConfirmationPage from './components/BusinessEventsConfirmationPage/BusinessEventsConfirmationPage';

interface IProps {
	businessEvent: IBusinessEvent;
	errors?: Record<string, any>;
	originalBusinessEvent: IBusinessEvent;
	setFieldValue: (
		field: string,
		value: any,
		shouldValidate?: boolean
	) => void;
	values: any;
}

const NAVIGATION_YEARS_RANGE = 2;

const BusinessEventsItemEditPage: React.FC<IProps> = ({
	businessEvent,
	errors,
	originalBusinessEvent,
	setFieldValue,
	values,
}) => {
	const {accountKey} = useParams<{accountKey: string}>();

	const [baseButtonDisabled, setBaseButtonDisabled] = useState<boolean>(true);

	const [reason, setReason] = useState('');

	const {businessEventTypesList, loading: loadingBusinessEventTypesList} =
		useGetBusinessEventTypesList();

	const emptyOption = useMemo(
		() => ({
			disabled: true,
			label: translate('select-the-option'),
			value: '',
		}),
		[]
	);

	const {hasAllEventsPermissions} = useHasAllEventsPermissions(
		accountKey || ''
	);

	const [hasImpactingEvents, setHasImpactingEvents] = useState(() => {
		return originalBusinessEvent.associatedTickets ? 'yes' : 'no';
	});

	const isDescriptionRequired = useMemo(
		() => businessEvent.eventType?.key === 'Other Event',
		[businessEvent.eventType]
	);

	const [isLoadingSubmitButton, setIsLoadingSubmitButton] =
		useState<boolean>(false);

	const [isModalOpen, setIsModalOpen] = useState(false);

	const isNewLiferayVersionRequired = useMemo(
		() => ['Migration', 'Upgrade'].includes(businessEvent.eventType?.key!),
		[businessEvent.eventType]
	);

	const {isSaasOnly} = useIsSaasOnly();

	const {loading: loadingTickets, tickets} = useAccountsTickets(
		originalBusinessEvent,
		accountKey || '',
		hasImpactingEvents === 'no'
	);

	const {loading: loadingUTCTimeZonesList, utcTimeZonesList} =
		useGetUTCTimeZonesList();

	const navigate = useNavigate();

	const now = new Date();

	const years = {
		end: now.getFullYear() + NAVIGATION_YEARS_RANGE,
		start: now.getFullYear(),
	};

	const {observer, onClose} = useModal({
		onClose: () => setIsModalOpen(false),
	});

	const {canViewTickets, loading: loadingJiraAccountChecking} =
		useCanViewTickets(accountKey || '');

	const {loading: loadingLiferayVersions, productVersions} =
		useGetLiferayVersions();

	const [newLiferayVersionOptions, setNewLiferayVersionOptions] = useState<
		IOption[]
	>([]);

	const [selectedTicketOptions, setSelectedTicketOptions] = useState<
		ITicket[]
	>([]);

	const [ticketOptions, setTicketOptions] = useState<ITicket[]>([]);

	const handleOptionChange = useCallback(
		(field: string, key: string, list: IOption[]) => {
			if (key) {
				setFieldValue(
					field,
					list.filter((option) => option.value === key)[0].label
				);
			}
		},
		[setFieldValue]
	);

	const handleRadioChange = (value: string) => {
		setHasImpactingEvents(value);
	};

	const handleRemove = useCallback((selectedTicket: ITicket) => {
		setTicketOptions((ticketOptions) => [
			...ticketOptions.map((ticket) => {
				return selectedTicket.ticketId === ticket.ticketId
					? {...ticket, selected: false}
					: {...ticket};
			}),
		]);

		setSelectedTicketOptions((selectedTicketOptions) => [
			...selectedTicketOptions.filter((ticket) => {
				return selectedTicket.ticketId !== ticket.ticketId;
			}),
		]);
	}, []);

	const handleSelect = useCallback((selectedTicket: ITicket) => {
		setTicketOptions((ticketOptions) => [
			...ticketOptions.map((ticket) => {
				return selectedTicket.ticketId === ticket.ticketId
					? {...ticket, selected: true}
					: {...ticket};
			}),
		]);

		setSelectedTicketOptions((selectedTicketOptions) => [
			...selectedTicketOptions,
			selectedTicket,
		]);
	}, []);

	const handleSubmit = async () => {
		const formattedBusinessEvent = {
			associatedTickets: businessEvent.associatedTickets,
			currentLiferayVersion: businessEvent.currentLiferayVersion?.key,
			description: businessEvent.description,
			eventStatus: originalBusinessEvent.eventStatus?.key,
			eventType: businessEvent.eventType?.key,
			lastComment: reason,
			name: businessEvent?.name,
			newLiferayVersion: businessEvent.newLiferayVersion?.key,
			plannedEventDate: getFormattedEventDateTime(
				businessEvent.plannedEventDate,
				businessEvent.plannedEventTime,
				businessEvent.timeZone?.key
			),
			timeZone: businessEvent.timeZone?.key,
		};

		try {
			setIsLoadingSubmitButton(true);

			if (!businessEvent.id) {
				throw new Error('Business event ID is missing');
			}

			await updateBusinessEvent(
				accountKey || '',
				businessEvent.id,
				formattedBusinessEvent
			);

			navigate(`/${accountKey}/business-events/${businessEvent.id}`);

			Liferay.Util.openToast({
				message: translate('the-changes-were-saved-successfully'),
				type: 'success',
			});
		}
		catch (error) {
			setIsLoadingSubmitButton(false);

			Liferay.Util.openToast({
				message: translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
	};

	const loading =
		loadingBusinessEventTypesList ||
		loadingJiraAccountChecking ||
		loadingLiferayVersions ||
		loadingUTCTimeZonesList;

	useEffect(() => {
		setFieldValue(
			'businessEvent.associatedTickets',
			hasImpactingEvents === 'yes'
				? selectedTicketOptions
						.map((ticket) => ticket.ticketId)
						.join(',')
				: ''
		);
	}, [hasImpactingEvents, selectedTicketOptions, setFieldValue]);

	useEffect(() => {
		if (!isDescriptionRequired) {
			setFieldValue('businessEvent.description', '');
		}
		else {
			originalBusinessEvent.description
				? setFieldValue(
						'businessEvent.description',
						originalBusinessEvent.description
					)
				: setFieldValue('businessEvent.description', '');
		}
	}, [
		isDescriptionRequired,
		originalBusinessEvent.description,
		setFieldValue,
	]);

	useEffect(() => {
		if (!isNewLiferayVersionRequired) {
			setFieldValue('businessEvent.newLiferayVersion.key', '');

			return;
		}

		if (
			businessEvent.newLiferayVersion?.key &&
			containsOption(
				newLiferayVersionOptions,
				businessEvent.newLiferayVersion
			)
		) {
			return;
		}

		if (
			originalBusinessEvent.newLiferayVersion &&
			containsOption(
				newLiferayVersionOptions,
				originalBusinessEvent.newLiferayVersion
			)
		) {
			setFieldValue(
				'businessEvent.newLiferayVersion.key',
				originalBusinessEvent.newLiferayVersion.key
			);

			setFieldValue(
				'businessEvent.newLiferayVersion.name',
				originalBusinessEvent.newLiferayVersion.name
			);

			return;
		}

		setFieldValue('businessEvent.newLiferayVersion.key', '');
	}, [
		businessEvent.newLiferayVersion,
		isNewLiferayVersionRequired,
		newLiferayVersionOptions,
		originalBusinessEvent.newLiferayVersion,
		setFieldValue,
	]);

	useEffect(() => {
		if (productVersions?.length) {
			setNewLiferayVersionOptions([
				...productVersions.filter((version, index, versions) => {
					if (businessEvent.currentLiferayVersion?.key) {
						return (
							index <
							versions.findIndex((version) => {
								return (
									version.value ===
									businessEvent.currentLiferayVersion?.key
								);
							})
						);
					}

					return true;
				}),
			]);
		}
	}, [
		businessEvent.currentLiferayVersion?.key,
		emptyOption,
		productVersions,
	]);

	useEffect(() => {
		if (originalBusinessEvent && tickets) {
			const associatedTickets = parseAssociatedTickets(
				originalBusinessEvent.associatedTickets
			);

			setTicketOptions([
				...tickets?.map((ticket) =>
					associatedTickets.includes(String(ticket.ticketId))
						? {...ticket, selected: true}
						: {...ticket, selected: false}
				),
			]);

			setSelectedTicketOptions([
				...tickets.filter((ticket) => {
					return associatedTickets.includes(String(ticket.ticketId));
				}),
			]);

			if (associatedTickets.length) {
				handleRadioChange('yes');
			}
		}
	}, [originalBusinessEvent, tickets]);

	useEffect(() => {
		const hasCurrentLiferayVersion =
			values.businessEvent.currentLiferayVersion.key;

		const hasDescription = values.businessEvent.description;
		const hasError = errors && Object.keys(errors).length;
		const hasEventName = values.businessEvent.name;
		const hasEventType = values.businessEvent.eventType.key;
		const hasNewLiferayVersion = values.businessEvent.newLiferayVersion.key;
		const hasPlannedEventDate = values.businessEvent.plannedEventDate;

		let hasAllRequiredFieldsFilled =
			Boolean(hasEventName) &&
			Boolean(hasEventType) &&
			Boolean(hasPlannedEventDate);

		if (isDescriptionRequired) {
			hasAllRequiredFieldsFilled =
				hasAllRequiredFieldsFilled && hasDescription;
		}

		if (isNewLiferayVersionRequired) {
			hasAllRequiredFieldsFilled =
				hasAllRequiredFieldsFilled && hasNewLiferayVersion;
		}

		if (!isSaasOnly) {
			hasAllRequiredFieldsFilled =
				hasAllRequiredFieldsFilled && hasCurrentLiferayVersion;
		}

		setBaseButtonDisabled(!hasAllRequiredFieldsFilled || Boolean(hasError));
	}, [
		errors,
		isDescriptionRequired,
		isNewLiferayVersionRequired,
		isSaasOnly,
		values.businessEvent.currentLiferayVersion,
		values.businessEvent.description,
		values.businessEvent.eventType,
		values.businessEvent.name,
		values.businessEvent.newLiferayVersion,
		values.businessEvent.plannedEventDate,
	]);

	return !loading ? (
		canViewTickets ? (
			hasAllEventsPermissions ? (
				<div className="be-edit-page">
					<div className="be-breadcrumbs font-weight-semi-bold mb-4">
						<span className="mx-2">
							<Link to={`/${accountKey}/business-events/`}>
								<ClayIcon
									className="mr-1"
									symbol="order-arrow-left"
								/>

								{translate('back-to-business-events')}
							</Link>
						</span>
					</div>

					<div>
						<div
							className={`align-items-center font-weight-semi-bold be-status be-status-${businessEvent?.eventStatus?.key.toLowerCase()} mb-1 d-inline px-2 py-1`}
						>
							{translate(
								getKebabCase(
									businessEvent?.eventStatus?.key as string
								) as any
							)}
						</div>

						<div className="align-items-center d-flex justify-content-between mb-4 mt-2">
							<div className="flex-fill font-weight-bold pr-4 text-neutral-10">
								<h3 className="mb-0 text-break">
									{businessEvent.name}
								</h3>
							</div>

							<div className="flex-shrink-0">
								<Button
									displayType="secondary"
									onClick={() => {
										navigate(
											`/${accountKey}/business-events/${businessEvent.id}`
										);
									}}
								>
									{translate('cancel')}
								</Button>

								<Button
									className="ml-3"
									disabled={
										baseButtonDisabled ||
										isLoadingSubmitButton
									}
									displayType="primary"
									isLoading={isLoadingSubmitButton}
									onClick={() => {
										const newPlannedEventDateTime =
											getFormattedEventDateTime(
												businessEvent.plannedEventDate,
												businessEvent.plannedEventTime,
												businessEvent.timeZone?.key
											);
										if (
											new Date(
												newPlannedEventDateTime || ''
											).getTime() !==
											new Date(
												originalBusinessEvent.plannedEventDate ||
													''
											).getTime()
										) {
											setIsModalOpen(true);
										}
										else {
											handleSubmit();
										}
									}}
								>
									{translate('save-changes')}
								</Button>
							</div>
						</div>
					</div>

					{isModalOpen && (
						<BusinessEventsConfirmationPage
							handleSubmit={handleSubmit}
							headerTitle={businessEvent.name!}
							isLoadingSubmitButton={isLoadingSubmitButton}
							message={translate(
								'we-understand-that-plans-change-please-let-us-know-why-the-planned-event-date-for-this-event-is-being-updated'
							)}
							observer={observer}
							onClose={onClose}
							reason={reason}
							setReason={setReason}
						/>
					)}

					<div className="mb-4">
						<NavigationBar
							fluidSize={false}
							triggerLabel={translate('event-details')}
						>
							<Nav.Item>
								<Nav.Link
									active={true}
									aria-label={`Switch to ${translate(
										'event-details'
									)}`}
									className="be-nav-link text-neutral-10"
								>
									{translate('event-details')}
								</Nav.Link>
							</Nav.Item>
						</NavigationBar>
					</div>
					<div className="event-edit-container">
						<FieldArray
							name="businessEvent"
							render={() => (
								<>
									<div className="event-edit-field mb-4">
										<Input
											badgeClassName="mt-1 mx-3"
											label={translate('event-name')}
											name="businessEvent.name"
											required
											type="text"
										/>
									</div>

									<div className="event-edit-field mb-4">
										<Select
											className="mx-3"
											groupStyle="pb-1"
											label={translate('event-type')}
											name="businessEvent.eventType.key"
											onChange={(value: string) =>
												handleOptionChange(
													'businessEvent.eventType.name',
													value,
													businessEventTypesList
												)
											}
											options={businessEventTypesList}
											required
										/>
									</div>

									{!isSaasOnly && (
										<div className="event-edit-field mb-4">
											<Select
												className="mx-3"
												groupStyle="pb-1"
												label={translate(
													'your-current-liferay-version'
												)}
												name="businessEvent.currentLiferayVersion.key"
												onChange={(value: string) =>
													handleOptionChange(
														'businessEvent.currentLiferayVersion.name',
														value,
														productVersions
													)
												}
												options={[
													emptyOption,
													...productVersions,
												]}
												required
											/>
										</div>
									)}

									{isNewLiferayVersionRequired && (
										<div className="event-edit-field mb-4">
											<Select
												badgeClassName="mx-3"
												className="mx-3"
												groupStyle="pb-1"
												label={translate('new-version')}
												name="businessEvent.newLiferayVersion.key"
												onChange={(value: string) =>
													handleOptionChange(
														'businessEvent.newLiferayVersion.name',
														value,
														newLiferayVersionOptions
													)
												}
												options={[
													emptyOption,
													...newLiferayVersionOptions,
												]}
												required
											/>
										</div>
									)}

									{isDescriptionRequired && (
										<div className="event-edit-field mb-4">
											<Input
												badgeClassName="mx-3"
												component="textarea"
												groupStyle="pb-1"
												label={translate(
													'event-description'
												)}
												name="businessEvent.description"
												placeholder={translate(
													'event-description'
												)}
												required
												type="text"
											/>
										</div>
									)}

									<div className="event-edit-field mb-4">
										<ClayInput.Group className="m-0">
											<ClayInput.GroupItem className="m-0">
												<DatePicker
													badgeClassName="mx-3"
													className="mx-3"
													dateFormat="MM-dd-yyyy"
													groupStyle="pb-1"
													label={translate(
														'planned-event-date'
													)}
													name="businessEvent.plannedEventDate"
													onChange={(
														value: string
													) => {
														setFieldValue(
															'businessEvent.plannedEventDate',
															value
														);
													}}
													placeholder={translate(
														'mm-dd-yyyy'
													)}
													required
													validations={[
														(value) =>
															isValidDate(
																value,
																years
															),
													]}
													years={years}
													yearsCheck
												/>
											</ClayInput.GroupItem>

											<ClayInput.GroupItem className="m-0">
												<Select
													groupStyle="pb-1"
													id="select-businessEvent.timeZone"
													label={translate(
														'time-zone'
													)}
													name="businessEvent.timeZone.key"
													onChange={(value: string) =>
														handleOptionChange(
															'businessEvent.timeZone.name',
															value,
															utcTimeZonesList
														)
													}
													options={[
														{
															...emptyOption,
															disabled: false,
														},
														...utcTimeZonesList,
													]}
													required
												/>
											</ClayInput.GroupItem>

											<ClayInput.GroupItem className="m-0">
												<TimePicker
													groupStyle="pb-1"
													label={translate('time')}
													name="businessEvent.plannedEventTime"
													onChange={(value) =>
														setFieldValue(
															'businessEvent.plannedEventTime',
															value
														)
													}
													required
												/>
											</ClayInput.GroupItem>
										</ClayInput.Group>
									</div>

									<div className="event-edit-field mx-3 pb-3">
										<label>
											{translate(
												'are-there-any-support-tickets-impacting-this-event'
											)}
										</label>

										<div className="ml-1">
											<ClayRadio
												checked={
													hasImpactingEvents === 'no'
												}
												label={translate('no')}
												onChange={() =>
													handleRadioChange('no')
												}
												value="no"
											/>

											<ClayRadio
												checked={
													hasImpactingEvents === 'yes'
												}
												label={translate('yes')}
												onChange={() =>
													handleRadioChange('yes')
												}
												value="yes"
											/>
										</div>
									</div>

									{hasImpactingEvents === 'yes' && (
										<div className="event-edit-field mx-3 pb-3">
											{loadingTickets ? (
												<ClayLoadingIndicator size="sm" />
											) : !!ticketOptions.length ||
											  !!selectedTicketOptions.length ? (
												<>
													<label>
														{translate(
															'please-select-the-tickets-that-are-impacting-this-event'
														)}
													</label>

													<div className="mr-3">
														<AssociatedTicketsContainer
															editing
															handleRemove={
																handleRemove
															}
															handleSelect={
																handleSelect
															}
															selectedTickets={
																selectedTicketOptions
															}
															ticketOptions={
																ticketOptions
															}
														/>
													</div>
												</>
											) : (
												<div className="mx-3 pb-3">
													{translate(
														'there-are-currently-no-open-tickets-under-this-project'
													)}
												</div>
											)}
										</div>
									)}
								</>
							)}
						/>
					</div>
				</div>
			) : (
				<p>
					{translate(
						'make-sure-the-project-link-is-correct-and-that-you-have-access-to-this-project'
					)}
				</p>
			)
		) : (
			<p
				dangerouslySetInnerHTML={{
					__html: translate(
						'we-apologize-for-the-inconvenience-but-we-ve-detected-a-system-error-with-this-project'
					),
				}}
			/>
		)
	) : (
		<div className="w-25">
			<ClayLoadingIndicator size="sm" />
		</div>
	);
};

const BusinessEventsItemEdit: React.FC = () => {
	const {accountKey, id} = useParams<{accountKey: string; id: string}>();

	const {businessEvent, loading} = useGetBusinessEvent(
		accountKey || '',
		id || ''
	);

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

	if (
		['Canceled', 'Completed'].includes(businessEvent.eventStatus?.key || '')
	) {
		return (
			<div className="h6 mt-4">
				{translate('cannot-edit-canceled-or-completed-events')}
			</div>
		);
	}

	const plannedEventDateISO =
		normalizeEventDateTime(
			businessEvent.plannedEventDate,
			businessEvent.timeZone?.key
		) ?? '';

	const [datePart = '', timePart = ''] = plannedEventDateISO.split('T');
	const plannedEventTime = timePart.substring(0, 5);
	const [year = '', month = '', day = ''] = datePart.split('-');

	return (
		<Formik
			initialValues={{
				businessEvent: {
					...businessEvent,
					currentLiferayVersion:
						businessEvent.currentLiferayVersion || {key: ''},
					newLiferayVersion: businessEvent.newLiferayVersion || {
						key: '',
					},
					plannedEventDate: datePart ? `${month}-${day}-${year}` : '',
					plannedEventTime: {
						hours: plannedEventTime?.split(':')[0] || '--',
						minutes: plannedEventTime?.split(':')[1] || '--',
					},
					timeZone: businessEvent.timeZone || {key: ''},
				},
			}}
			onSubmit={() => {}}
			validateOnChange
		>
			{(formikProps) => (
				<BusinessEventsItemEditPage
					businessEvent={
						formikProps.values
							.businessEvent as unknown as IBusinessEvent
					}
					errors={formikProps.errors}
					originalBusinessEvent={businessEvent}
					setFieldValue={formikProps.setFieldValue}
					values={formikProps.values}
				/>
			)}
		</Formik>
	);
};

export default BusinessEventsItemEdit;
