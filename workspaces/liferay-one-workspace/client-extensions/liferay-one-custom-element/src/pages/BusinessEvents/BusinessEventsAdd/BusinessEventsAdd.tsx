/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput, ClayRadio} from '@clayui/form';
import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useCallback, useEffect, useMemo, useState} from 'react';
import {
	Controller,
	FormProvider,
	useForm,
	useFormContext,
} from 'react-hook-form';
import {useNavigate, useParams} from 'react-router-dom';
import Button from '~/components/Button/Button';
import DatePicker from '~/components/DatePicker/DatePicker';
import TimePicker from '~/components/TimePicker/TimePicker';
import {translate} from '~/i18n';
import AssociatedTicketsContainer from '~/pages/BusinessEvents/components/AssociatedTicketsContainer/AssociatedTicketsContainer';
import Input from '~/pages/BusinessEvents/components/Input/Input';
import Select, {IOption} from '~/pages/BusinessEvents/components/Select/Select';
import useCanViewTickets from '~/pages/BusinessEvents/hooks/useCanViewTickets';
import useGetBusinessEventTypesList from '~/pages/BusinessEvents/hooks/useGetBusinessEventTypesList';
import useGetLiferayVersions from '~/pages/BusinessEvents/hooks/useGetLiferayVersions';
import useGetUTCTimeZonesList from '~/pages/BusinessEvents/hooks/useGetUTCTimeZonesList';
import useHasAllEventsPermissions from '~/pages/BusinessEvents/hooks/useHasAllEventsPermissions';
import useIsSaasOnly from '~/pages/BusinessEvents/hooks/useIsSaasOnly';
import useProjectTickets from '~/pages/BusinessEvents/hooks/useProjectTickets';
import {IBusinessEvent} from '~/pages/BusinessEvents/types';
import {containsOption} from '~/pages/BusinessEvents/utils/containsOption';
import {getFormattedEventDateTime} from '~/pages/BusinessEvents/utils/getFormattedEventDateUtils';
import getInitialEvent from '~/pages/BusinessEvents/utils/getInitialEvent';
import {Liferay} from '~/services/liferay/liferay';
import {createBusinessEvent} from '~/services/spring-boot/Jira';
import {ITicket} from '~/types/ticket';
import {isValidDate, requiredTimeInput} from '~/utils/formValidationUtils';

import FormLayout from './components/FormLayout/FormLayout';

const NAVIGATION_YEARS_RANGE = 2;

const BusinessEventsAddPage: React.FC = () => {
	const {projectERC} = useParams<{projectERC: string}>();

	const {
		control,
		formState: {errors, touchedFields: touched},
		setValue,
		watch,
	} = useFormContext();

	const values = watch();
	const businessEvent = values.businessEvent as unknown as IBusinessEvent;

	const setFieldValue = useCallback(
		(field: string, value: unknown) =>
			setValue(
				field as Parameters<typeof setValue>[0],
				value as Parameters<typeof setValue>[1],
				{
					shouldDirty: true,
					shouldTouch: true,
					shouldValidate: true,
				}
			),
		[setValue]
	);

	const [baseButtonDisabled, setBaseButtonDisabled] = useState<boolean>(true);

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
		projectERC || ''
	);

	const [hasImpactingEvents, setHasImpactingEvents] = useState<string>('no');

	const isDescriptionRequired = useMemo(
		() => businessEvent.eventType?.key === 'Other Event',
		[businessEvent.eventType]
	);

	const [isLoadingSubmitButton, setIsLoadingSubmitButton] =
		useState<boolean>(false);

	const isNewLiferayVersionRequired = useMemo(
		() => ['Migration', 'Upgrade'].includes(businessEvent.eventType?.key!),
		[businessEvent.eventType]
	);

	const {isSaasOnly} = useIsSaasOnly();

	const {loading: loadingTickets, tickets} = useProjectTickets(
		undefined,
		projectERC || '',
		hasImpactingEvents === 'no'
	);

	const {canViewTickets, loading: loadingJiraAccountChecking} =
		useCanViewTickets(projectERC || '');

	const {loading: loadingUTCTimeZonesList, utcTimeZonesList} =
		useGetUTCTimeZonesList();

	const navigate = useNavigate();

	const now = new Date();

	const years = {
		end: now.getFullYear() + NAVIGATION_YEARS_RANGE,
		start: now.getFullYear(),
	};

	const {loading: loadingLiferayVersions, productVersions} =
		useGetLiferayVersions();

	const [newLiferayVersionOptions, setNewLiferayVersionOptions] = useState<
		IOption[]
	>([]);

	const [selectedTickets, setSelectedTickets] = useState<ITicket[]>([]);

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

		setSelectedTickets((selectedTickets) => [
			...selectedTickets.filter((ticket) => {
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

		setSelectedTickets((selectedTickets) => [
			...selectedTickets,
			selectedTicket,
		]);
	}, []);

	const handleSubmit = async () => {
		const updatedBusinessEvent = {
			...businessEvent,
			currentLiferayVersion: businessEvent.currentLiferayVersion?.key,
			eventStatus: businessEvent.eventStatus?.key,
			eventType: businessEvent.eventType?.name,
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

			await createBusinessEvent(updatedBusinessEvent, projectERC || '');

			navigate(`/${projectERC}/business-events`);

			Liferay.Util.openToast({
				message: translate('business-event-created-successfully'),
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
				? selectedTickets.map((ticket) => ticket.ticketId).join(',')
				: ''
		);
	}, [hasImpactingEvents, selectedTickets, setFieldValue]);

	useEffect(() => {
		const hasCurrentLiferayVersion =
			values.businessEvent.currentLiferayVersion.key;

		const hasDescription = values.businessEvent.description;
		const hasError = errors && Object.keys(errors).length;
		const hasEventName = values.businessEvent.name;
		const hasEventType = values.businessEvent.eventType.key;
		const hasNewLiferayVersion = values.businessEvent.newLiferayVersion.key;
		const hasPlannedEventDate = values.businessEvent.plannedEventDate;
		const hasTouched = Boolean(Object.keys(touched).length);

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

		setBaseButtonDisabled(
			!hasAllRequiredFieldsFilled || Boolean(hasError) || !hasTouched
		);
	}, [
		errors,
		isDescriptionRequired,
		isNewLiferayVersionRequired,
		isSaasOnly,
		touched,
		values.businessEvent.currentLiferayVersion,
		values.businessEvent.description,
		values.businessEvent.eventType,
		values.businessEvent.name,
		values.businessEvent.newLiferayVersion,
		values.businessEvent.plannedEventDate,
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
		if (!isDescriptionRequired) {
			setFieldValue('businessEvent.description', '');
		}
	}, [isDescriptionRequired, setFieldValue]);

	useEffect(() => {
		if (
			!isNewLiferayVersionRequired ||
			!containsOption(
				newLiferayVersionOptions,
				businessEvent.newLiferayVersion?.key
			)
		) {
			setFieldValue('businessEvent.newLiferayVersion.key', '');
		}
	}, [
		businessEvent.newLiferayVersion?.key,
		isNewLiferayVersionRequired,
		newLiferayVersionOptions,
		setFieldValue,
	]);

	useEffect(() => {
		setTicketOptions([
			...(tickets?.map((ticket) => {
				return {...ticket, selected: false};
			}) || []),
		]);
	}, [tickets]);

	return !loading ? (
		canViewTickets ? (
			hasAllEventsPermissions ? (
				<FormLayout
					footerProps={{
						leftButton: (
							<Button
								displayType="secondary"
								onClick={() => {
									navigate(`/${projectERC}/business-events`);
								}}
							>
								{translate('cancel')}
							</Button>
						),
						middleButton: (
							<Button
								disabled={
									baseButtonDisabled || isLoadingSubmitButton
								}
								displayType="primary"
								isLoading={isLoadingSubmitButton}
								onClick={handleSubmit}
							>
								{translate('create-event')}
							</Button>
						),
					}}
					headerProps={{
						title: translate('create-business-event'),
					}}
					layoutType="cp-required-info"
				>
					<Controller
						control={control}
						name="businessEvent.name"
						render={({field, fieldState}) => (
							<Input
								{...field}
								badgeClassName="mt-1 mx-3"
								error={fieldState.error?.message}
								label={translate('event-name')}
								placeholder={translate('event-name')}
								required
								type="text"
							/>
						)}
						rules={{required: translate('this-field-is-required')}}
					/>

					<Controller
						control={control}
						name="businessEvent.eventType.key"
						render={({field, fieldState}) => (
							<Select
								badgeClassName="mt-1 mx-3"
								error={fieldState.error?.message}
								label={translate('event-type')}
								onBlur={field.onBlur}
								onChange={(value) => {
									field.onChange(value);
									handleOptionChange(
										'businessEvent.eventType.name',
										value,
										businessEventTypesList
									);
								}}
								options={[
									emptyOption,
									...businessEventTypesList,
								]}
								required
								value={field.value}
							/>
						)}
						rules={{required: translate('this-field-is-required')}}
					/>

					{!isSaasOnly && (
						<Controller
							control={control}
							name="businessEvent.currentLiferayVersion.key"
							render={({field, fieldState}) => (
								<Select
									badgeClassName="mt-1 mx-3"
									error={fieldState.error?.message}
									label={translate(
										'your-current-liferay-version'
									)}
									onBlur={field.onBlur}
									onChange={(value) => {
										field.onChange(value);
										handleOptionChange(
											'businessEvent.currentLiferayVersion.name',
											value,
											productVersions
										);
									}}
									options={[emptyOption, ...productVersions]}
									required
									value={field.value}
								/>
							)}
							rules={{
								required: translate('this-field-is-required'),
							}}
						/>
					)}

					{isNewLiferayVersionRequired && (
						<Controller
							control={control}
							name="businessEvent.newLiferayVersion.key"
							render={({field, fieldState}) => (
								<Select
									badgeClassName="mt-1 mx-3"
									error={fieldState.error?.message}
									label={translate('new-version')}
									onBlur={field.onBlur}
									onChange={(value) => {
										field.onChange(value);
										handleOptionChange(
											'businessEvent.newLiferayVersion.name',
											value,
											newLiferayVersionOptions
										);
									}}
									options={[
										emptyOption,
										...newLiferayVersionOptions,
									]}
									required
									value={field.value}
								/>
							)}
							rules={{
								required: translate('this-field-is-required'),
							}}
						/>
					)}

					{isDescriptionRequired && (
						<Controller
							control={control}
							name="businessEvent.description"
							render={({field, fieldState}) => (
								<Input
									{...field}
									badgeClassName="mt-1 mx-3"
									component="textarea"
									error={fieldState.error?.message}
									label={translate('event-description')}
									placeholder={translate('event-description')}
									required
									type="text"
								/>
							)}
							rules={{
								required: translate('this-field-is-required'),
							}}
						/>
					)}

					<ClayInput.Group className="m-0">
						<ClayInput.GroupItem className="m-0">
							<Controller
								control={control}
								name="businessEvent.plannedEventDate"
								render={({field, fieldState}) => (
									<DatePicker
										badgeClassName="mt-1 mx-3"
										dateFormat="MM-dd-yyyy"
										error={fieldState.error?.message}
										label={translate('planned-event-date')}
										onBlur={field.onBlur}
										onChange={field.onChange}
										placeholder={translate('mm-dd-yyyy')}
										required
										value={field.value}
										years={years}
										yearsCheck
									/>
								)}
								rules={{
									required: translate(
										'this-field-is-required'
									),
									validate: (value) =>
										isValidDate(value, years) || true,
								}}
							/>
						</ClayInput.GroupItem>

						<ClayInput.GroupItem className="m-0">
							<Controller
								control={control}
								name="businessEvent.timeZone.key"
								render={({field, fieldState}) => (
									<Select
										error={fieldState.error?.message}
										id="select-businessEvent.timeZone"
										label={translate('time-zone')}
										onBlur={field.onBlur}
										onChange={(value) => {
											field.onChange(value);
											handleOptionChange(
												'businessEvent.timeZone.name',
												value,
												utcTimeZonesList
											);
										}}
										options={[
											{
												...emptyOption,
												disabled: false,
											},
											...utcTimeZonesList,
										]}
										required
										value={field.value}
									/>
								)}
								rules={{
									required: translate(
										'this-field-is-required'
									),
								}}
							/>
						</ClayInput.GroupItem>

						<ClayInput.GroupItem className="m-0">
							<Controller
								control={control}
								name="businessEvent.plannedEventTime"
								render={({field, fieldState}) => (
									<TimePicker
										error={fieldState.error?.message}
										label={translate('time')}
										onBlur={field.onBlur}
										onChange={field.onChange}
										required
										value={field.value}
									/>
								)}
								rules={{
									validate: (value) =>
										requiredTimeInput(value) || true,
								}}
							/>
						</ClayInput.GroupItem>
					</ClayInput.Group>

					<div className="mx-3 pb-3">
						<label>
							{translate(
								'are-there-any-support-tickets-impacting-this-event'
							)}
						</label>

						<div className="ml-1">
							<ClayRadio
								checked={hasImpactingEvents === 'no'}
								label={translate('no')}
								onChange={() => handleRadioChange('no')}
								value="no"
							/>

							<ClayRadio
								checked={hasImpactingEvents === 'yes'}
								label={translate('yes')}
								onChange={() => handleRadioChange('yes')}
								value="yes"
							/>
						</div>
					</div>

					{hasImpactingEvents === 'yes' && (
						<div className="mx-3 pb-3">
							{loadingTickets ? (
								<ClayLoadingIndicator size="sm" />
							) : ticketOptions.length ? (
								<>
									<label>
										{translate(
											'please-select-the-tickets-that-are-impacting-this-event'
										)}
									</label>

									<AssociatedTicketsContainer
										editing
										handleRemove={handleRemove}
										handleSelect={handleSelect}
										selectedTickets={selectedTickets}
										ticketOptions={ticketOptions}
									/>
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
				</FormLayout>
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
		<div className="mx-auto">
			<ClayLoadingIndicator size="sm" />
		</div>
	);
};

const BusinessEventsAdd: React.FC = () => {
	const methods = useForm({
		defaultValues: {businessEvent: getInitialEvent()},
		mode: 'onChange',
	});

	return (
		<FormProvider {...methods}>
			<BusinessEventsAddPage />
		</FormProvider>
	);
};

export default BusinessEventsAdd;
