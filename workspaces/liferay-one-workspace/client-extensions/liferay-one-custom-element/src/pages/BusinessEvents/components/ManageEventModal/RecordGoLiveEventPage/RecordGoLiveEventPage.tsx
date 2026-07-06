/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import {Observer} from '@clayui/modal/lib/types';
import classNames from 'classnames';
import {useEffect, useMemo, useState} from 'react';
import {Controller, useFormContext} from 'react-hook-form';
import Badge from '~/components/Badge/Badge';
import DatePicker from '~/components/DatePicker/DatePicker';
import TimePicker from '~/components/TimePicker/TimePicker';
import {translate} from '~/i18n';
import BusinessEventsModal from '~/pages/BusinessEvents/components/BusinessEventsModal/BusinessEventsModal';
import Select, {IOption} from '~/pages/BusinessEvents/components/Select/Select';
import useGetUTCTimeZonesList from '~/pages/BusinessEvents/hooks/useGetUTCTimeZonesList';
import {IBusinessEvent} from '~/pages/BusinessEvents/types';
import {getFormattedEventDateTime} from '~/pages/BusinessEvents/utils/getFormattedEventDateUtils';
import {Liferay} from '~/services/liferay/liferay';
import {updateBusinessEvent} from '~/services/spring-boot/Jira';

interface IProps {
	businessEvent: IBusinessEvent;
	closeFunction?: (value: boolean) => void;
	modalType: string;
	observer: Observer;
	onCompleted: () => void;
	projectExternalReferenceCode: string;
}

const RecordGoLiveEventPage: React.FC<IProps> = ({
	businessEvent,
	closeFunction = () => {},
	modalType,
	observer,
	onCompleted,
	projectExternalReferenceCode,
}) => {
	const {
		control,
		formState: {errors},
		setValue,
		watch,
	} = useFormContext();

	const values = watch();

	const setFieldValue = (field: string, value: unknown) =>
		setValue(
			field as Parameters<typeof setValue>[0],
			value as Parameters<typeof setValue>[1],
			{
				shouldDirty: true,
				shouldTouch: true,
				shouldValidate: true,
			}
		);

	const [baseButtonDisabled, setBaseButtonDisabled] = useState<boolean>(true);
	const [isLoadingSubmitButton, setIsLoadingSubmitButton] =
		useState<boolean>(false);
	const [isValidRecordDate, setIsValidRecordDate] = useState<boolean>(false);

	const emptyOption = useMemo(
		() => ({
			disabled: true,
			label: translate('select-the-option'),
			value: '',
		}),
		[]
	);

	const {utcTimeZonesList} = useGetUTCTimeZonesList();
	const [utcTimeZonesOptions, setUTCTimeZonesOptions] = useState<IOption[]>(
		[]
	);

	const handleInputChange = (event: {target: {value: string}}) => {
		setFieldValue('businessEvent.lastComment', event.target.value);
	};

	useEffect(() => {
		if (utcTimeZonesList?.length) {
			setUTCTimeZonesOptions([
				{...emptyOption, disabled: false},
				...utcTimeZonesList,
			]);
		}
	}, [emptyOption, utcTimeZonesList]);

	useEffect(() => {
		const hasError = errors && Object.keys(errors).length;
		const hasActualEventDate = values.businessEvent?.actualEventDate;
		const hasActualEventTime = values.businessEvent?.actualEventTime;
		const hasTimeZone = values.businessEvent?.timeZone?.key;

		const isDateValid = (date: string) => new Date(date) <= new Date();
		const isActualEventDateValid = isDateValid(hasActualEventDate);

		setIsValidRecordDate(isActualEventDateValid);

		const hasAllRequiredFieldsFilled =
			Boolean(hasActualEventDate) &&
			Boolean(hasActualEventTime) &&
			Boolean(hasTimeZone);

		setBaseButtonDisabled(
			!hasAllRequiredFieldsFilled ||
				Boolean(hasError) ||
				!isActualEventDateValid
		);
	}, [
		errors,
		values.businessEvent?.actualEventDate,
		values.businessEvent?.actualEventTime,
		values.businessEvent?.timeZone?.key,
	]);

	const handleSubmit = async () => {
		const businessEventId = businessEvent.id;

		if (!businessEventId) {
			return;
		}

		const updatedBusinessEvent = {...values?.businessEvent};
		const formattedBusinessEvent = {
			...businessEvent,
			actualEventDate: getFormattedEventDateTime(
				updatedBusinessEvent.actualEventDate,
				updatedBusinessEvent.actualEventTime,
				updatedBusinessEvent.timeZone?.key
			),
			currentLiferayVersion: businessEvent.currentLiferayVersion?.key,
			eventStatus: 'Completed',
			eventType: businessEvent.eventType?.key,
			lastComment: updatedBusinessEvent?.lastComment,
			newLiferayVersion: businessEvent.newLiferayVersion?.key,
			timeZone: updatedBusinessEvent.timeZone?.key,
		};

		try {
			setIsLoadingSubmitButton(true);

			await updateBusinessEvent(
				formattedBusinessEvent,
				businessEventId,
				projectExternalReferenceCode
			);

			closeFunction(false);

			onCompleted();
		}
		catch (error) {
			setIsLoadingSubmitButton(false);

			Liferay.Util.openToast({
				message: translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
	};

	const isEditable = ['Open', 'Overdue'].includes(
		businessEvent.eventStatus?.key!
	);

	return (
		<BusinessEventsModal
			baseButtonDisabled={baseButtonDisabled}
			handleSubmit={handleSubmit}
			headerTitle={businessEvent.name!}
			isLoadingSubmitButton={isLoadingSubmitButton}
			modalType={modalType}
			observer={observer}
			onClose={() => closeFunction(false)}
			submitButton={translate('record-actual-event-date')}
			title={translate('record-actual-event-date')}
		>
			{isEditable ? (
				<div>
					<ClayInput.Group className="business-date-container m-0">
						<ClayInput.GroupItem
							className={classNames('m-0', {
								'be-record-container': !isValidRecordDate,
							})}
						>
							<Controller
								control={control}
								name="businessEvent.actualEventDate"
								render={({field, fieldState}) => (
									<DatePicker
										badgeClassName="mr-4"
										dateFormat="MM-dd-yyyy"
										error={fieldState.error?.message}
										groupStyle="pb-1"
										label={translate('actual-event-date')}
										onBlur={field.onBlur}
										onChange={field.onChange}
										placeholder={translate('mm-dd-yyyy')}
										required
										value={field.value}
									/>
								)}
							/>
						</ClayInput.GroupItem>

						<ClayInput.GroupItem className="m-0">
							<Controller
								control={control}
								name="businessEvent.timeZone.key"
								render={({field, fieldState}) => (
									<Select
										error={fieldState.error?.message}
										groupStyle="pb-1"
										id="select-businessEvent.timeZone"
										label={translate('time-zone')}
										onBlur={field.onBlur}
										onChange={field.onChange}
										options={utcTimeZonesOptions}
										required
										value={field.value}
									/>
								)}
							/>
						</ClayInput.GroupItem>

						<ClayInput.GroupItem className="m-0">
							<Controller
								control={control}
								name="businessEvent.actualEventTime"
								render={({field, fieldState}) => (
									<TimePicker
										error={fieldState.error?.message}
										groupStyle="pb-1"
										label={translate('time')}
										onBlur={field.onBlur}
										onChange={field.onChange}
										required
										value={field.value}
									/>
								)}
							/>
						</ClayInput.GroupItem>
					</ClayInput.Group>

					<div className="font-weight-bold mb-3">
						{translate(
							'please-let-us-know-if-you-have-any-feedback-on-the-support-you-received-during-this-time'
						)}
					</div>

					<ClayInput
						component="textarea"
						onChange={handleInputChange}
						required
						type="text"
						value={values.businessEvent?.lastComment}
					/>

					<Badge alertType="info" badgeClassName="mt-3">
						<span className="pl-1 text-paragraph">
							{translate(
								'entering-an-actual-event-date-will-close-this-business-event-no-further-edits-will-be-possible'
							)}
						</span>
					</Badge>

					{values.businessEvent?.actualEventDate! &&
						!isValidRecordDate && (
							<Badge>
								<span className="pl-1">
									{translate(
										'please-select-an-actual-event-date-that-has-already-occurred-or-is-today'
									)}
								</span>
							</Badge>
						)}
				</div>
			) : (
				<div className="h6 my-4">
					{translate('cannot-edit-canceled-or-completed-events')}
				</div>
			)}
		</BusinessEventsModal>
	);
};

export default RecordGoLiveEventPage;
