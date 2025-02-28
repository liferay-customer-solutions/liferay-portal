/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput, ClayRadio, ClaySelect} from '@clayui/form';

import './BusinessEventsItemEdition.css';

import ClayMultiSelect from '@clayui/multi-select';
import {FieldArray, Formik} from 'formik';
import {useEffect, useState} from 'react';
import {DatePicker, Select, TimePicker} from '~/components';
import useGetBusinessEventTypesList from '~/features/project/containers/BusinessEventForm/hooks/useGetBusinessEventTypesList';
import useGetGMTTimeZonesList from '~/features/project/containers/BusinessEventForm/hooks/useGetGMTTimeZonesList';
import useGetVersionOfLiferaySoftwareList from '~/features/project/containers/BusinessEventForm/hooks/useGetVersionOfLiferaySoftwareList';
import {updateBusinessEventItem} from '~/services/liferay/api';
import i18n from '~/utils/I18n';
import getInitialEvent from '~/utils/getInitialEvent';

interface IBusinessEventValues {
	businessEvent: {
		currentLiferayVersion: string;
		eventType: string;
		newLiferayVersion: string;
		targetGoLiveDate: string;
		time: string;
		timeZone: string;
	};
}

const BusinessEventsItemEditionForm = ({
	setFieldValue,
	setSubmitTriggered,
	submitTriggered,
	ticketID,
	values,
}: {
	setFieldValue: (field: string, value: any) => void;
	setSubmitTriggered: any;
	submitTriggered: any;
	ticketID: string | number;
	values: IBusinessEventValues;
}) => {
	const [hasImpactingEvents, setHasImpactingEvents] = useState<string>('no');
	const [time, setTime] = useState({
		hours: '--',
		minutes: '--',
	});
	const [businessEventTypesOptions, setBusinessEventTypesOptions] = useState<
		any[]
	>([]);
	const [gmtTimeZonesOptions, setGMTTimeZonesOptions] = useState<any[]>([]);
	const [
		versionOfLiferaySoftwareOptions,
		setVersionOfLiferaySoftwareOptions,
	] = useState<any[]>([]);

	const businessEventTypesList = useGetBusinessEventTypesList();
	const gmtTimeZonesList = useGetGMTTimeZonesList();
	const versionOfLiferaySoftwareList = useGetVersionOfLiferaySoftwareList();

	const handleRadioChange = (value: string) => {
		setHasImpactingEvents(value);
	};

	const handleTimeChange = (newTime: any) => {
		setTime(newTime);
	};

	const formatLiferayVersion = (version: string) => {
		if (version.startsWith('liferayPortal')) {
			const versionNumber = version.replace('liferayPortal', '');

			return `Liferay Portal ${versionNumber[0]}.${versionNumber[1]}`;
		}

		if (version.startsWith('liferayDXP')) {
			const versionNumber = version.replace('liferayDxp', '');

			return `Liferay DXP ${versionNumber[0]}.${versionNumber[1]}`;
		}

		return version;
	};

	const formatEventType = (eventType: string) => {
		if (eventType === 'golive') {
			return 'Go-Live';
		}

		if (eventType === 'otherevent') {
			return 'Other Event';
		}

		return eventType.charAt(0).toUpperCase() + eventType.slice(1);
	};

	const formatDateAndTime = (
		date: string,
		timeZone: string,
		time: string
	) => {
		const [month, day, year] = date.split('/');
		const formattedDate = new Date(`${year}-${month}-${day}T00:00:00`);

		const [hours, minutes] = time.split(':');
		formattedDate.setHours(parseInt(hours));
		formattedDate.setMinutes(parseInt(minutes));

		const offset = parseInt(timeZone.replace('GMT', '').replace(':', ''));
		formattedDate.setHours(formattedDate.getHours() + offset);

		const formattedTimestamp = formattedDate.toISOString();

		return formattedTimestamp;
	};

	const handleSubmit = async (values: any) => {
		const formattedEventTime = formatDateAndTime(
			values.businessEvent.targetGoLiveDate,
			values.businessEvent.timeZone,
			values.businessEvent.time
		);

		const fieldsToPatch = {
			currentLiferayVersion: {
				key: values.businessEvent.currentLiferayVersion,
				name: formatLiferayVersion(
					values.businessEvent.currentLiferayVersion
				),
			},
			eventType: {
				key: values.businessEvent.eventType,
				name: formatEventType(values.businessEvent.eventType),
			},
			newLiferayVersion: {
				key: values.businessEvent.newLiferayVersion,
				name: formatLiferayVersion(
					values.businessEvent.newLiferayVersion
				),
			},
			targetGoLiveDateTime: formattedEventTime,
		};

		setSubmitTriggered(true);
		await updateBusinessEventItem(ticketID, fieldsToPatch);
	};

	useEffect(() => {
		if (submitTriggered) {
			handleSubmit(values);
			setSubmitTriggered(false);
		}
	}, [submitTriggered, setSubmitTriggered]);

	const emptyOption = {
		disabled: true,
		label: i18n.translate('select-the-option'),
		value: '',
	};

	useEffect(() => {
		if (businessEventTypesList?.length) {
			setBusinessEventTypesOptions([
				emptyOption,
				...businessEventTypesList,
			]);
		}
	}, [businessEventTypesList]);

	useEffect(() => {
		if (gmtTimeZonesList?.length) {
			setGMTTimeZonesOptions([emptyOption, ...gmtTimeZonesList]);
		}
	}, [gmtTimeZonesList]);

	useEffect(() => {
		if (versionOfLiferaySoftwareList?.length) {
			setVersionOfLiferaySoftwareOptions([
				emptyOption,
				...versionOfLiferaySoftwareList,
			]);
		}
	}, [versionOfLiferaySoftwareList]);

	useEffect(() => {
		if (businessEventTypesOptions?.length) {
			setFieldValue(
				'businessEvent.eventType',
				businessEventTypesOptions[0].value
			);
		}
	}, [businessEventTypesOptions]);

	useEffect(() => {
		if (versionOfLiferaySoftwareOptions?.length) {
			setFieldValue(
				'businessEvent.currentLiferayVersion',
				versionOfLiferaySoftwareOptions[0].value
			);
		}
	}, [versionOfLiferaySoftwareOptions]);

	return (
		<div className="event-edit-container">
			<FieldArray
				name="businessEvent"
				render={() => (
					<>
						<div className="event-edit-field mb-4">
							<Select
								badgeClassName="ml-3 mr-3"
								groupStyle="pb-1"
								label={i18n.translate('event-type')}
								name="businessEvent.eventType"
								options={businessEventTypesOptions}
								required
							/>
						</div>

						<div className="event-edit-field mb-4">
							<Select
								badgeClassName="ml-3 mr-3"
								groupStyle="pb-1"
								label={i18n.translate(
									'your-current-liferay-version'
								)}
								name="businessEvent.currentLiferayVersion"
								options={versionOfLiferaySoftwareOptions}
								required
							/>
						</div>

						<div className="event-edit-field mb-4">
							<Select
								badgeClassName="ml-3 mr-3"
								groupStyle="pb-1"
								label={i18n.translate('new-version')}
								name="businessEvent.newLiferayVersion"
								options={versionOfLiferaySoftwareOptions}
								required
							/>
						</div>

						<div className="event-edit-field mb-4">
							<ClayInput.Group className="m-0">
								<ClayInput.GroupItem className="m-0">
									<DatePicker
										badgeClassName="ml-3 mr-3"
										dateFormat="MM/dd/yyyy"
										groupStyle="pb-1"
										label={i18n.translate(
											'target-go-live-date'
										)}
										name="businessEvent.targetGoLiveDate"
										onChange={(value) =>
											setFieldValue(
												'businessEvent.targetGoLiveDate',
												value
											)
										}
										placeholder={i18n.translate(
											'mm-dd-yyyy'
										)}
										value={
											values.businessEvent
												.targetGoLiveDate
										}
									/>
								</ClayInput.GroupItem>

								<ClayInput.GroupItem className="m-0">
									<Select
										groupStyle="pb-1"
										id="select-businessEvent.timeZone"
										label={i18n.translate('time-zone')}
										name="businessEvent.timeZone"
										options={gmtTimeZonesOptions}
									/>
								</ClayInput.GroupItem>

								<ClayInput.GroupItem className="m-0">
									<TimePicker
										groupStyle="pb-1"
										label={i18n.translate('time')}
										name="businessEvent.time"
										onChange={handleTimeChange}
										value={time}
									/>
								</ClayInput.GroupItem>
							</ClayInput.Group>
						</div>

						<div className="event-edit-field mb-4">
							<div>
								{i18n.translate(
									'are-there-any-support-tickets-impacting-this-event'
								)}
							</div>
							<div>
								<ClayRadio
									checked={hasImpactingEvents === 'no'}
									label="No"
									onChange={() => handleRadioChange('no')}
									value="no"
								/>
								<ClayRadio
									checked={hasImpactingEvents === 'yes'}
									label="Yes"
									onChange={() => handleRadioChange('yes')}
									value="yes"
								/>
							</div>
						</div>

						{hasImpactingEvents === 'yes' && (
							<div className="event-edit-field mb-4">
								<div>
									{i18n.translate(
										'please-select-the-tickets-that-are-impacting-this-event'
									)}
								</div>
								<ClayMultiSelect
									value={i18n.translate('ticket')}
								>
									<ClaySelect.Option
										label={i18n.translate('ticket-1')}
										value="ticket-1"
									/>
									<ClaySelect.Option
										label={i18n.translate('ticket-2')}
										value="ticket-2"
									/>
								</ClayMultiSelect>
							</div>
						)}
					</>
				)}
			/>
		</div>
	);
};

const BusinessEventsItemEdition = ({ticketID, ...props}: any) => {
	return (
		<Formik
			initialValues={{businessEvent: getInitialEvent()}}
			onSubmit={(values) => console.log(values)}
			validateOnChange
		>
			{(formikProps) => (
				<BusinessEventsItemEditionForm
					{...props}
					{...formikProps}
					ticketID={ticketID}
				/>
			)}
		</Formik>
	);
};

export default BusinessEventsItemEdition;
