/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {zodResolver} from '@hookform/resolvers/zod';

import {useOneContext} from '../../../context/OneContextProvider';
import useListTypeDefinition from '../../../hooks/useListTypeDefinition';
import i18n from '~/i18n';
import publishingSchemas from '../../../schema/publishingSchemas';
import fetcher from '../../../services/fetcher/fetcher';
import PublisherGateForm from './PublisherGateForm';
import PublisherGateSummary from './PublisherGateSummary';
import PubliserhRequestedCard from './PublisherRequestedCard';
import PublisherSummaryContent from './PublisherSummaryContent';

export type PublisherForm = z.infer<typeof publishingSchemas.becomePublisherForm>;

export enum PublisherGateStep {
	FORM = 'form',
	SUMMARY = 'summary',
	REQUESTED = 'requested',
}

const PublisherGateSteps = () => {
	const {myUserAccount} = useOneContext();
	const [step, setStep] = useState<PublisherGateStep>(PublisherGateStep.FORM);
	const userPhone =
		myUserAccount?.userAccountContactInformation?.telephones || [];

	const form = useForm<PublisherForm>({
		defaultValues: {
			emailAddress: myUserAccount ? myUserAccount?.emailAddress : '',
			extension: userPhone?.length ? userPhone[0]?.extension : '',
			firstName: myUserAccount ? myUserAccount?.givenName : '',
			lastName: myUserAccount ? myUserAccount?.familyName : '',
			phone: {
				code: '+1',
				flag: 'en-us',
			},
			phoneNumber: userPhone?.length ? userPhone[0]?.phoneNumber : '',
			publisherType: ['appPublisher'],
			requestDescription: '',
		},
		mode: 'onBlur',
		resolver: zodResolver(publishingSchemas.becomePublisherForm),
	});

	useEffect(() => {
		if (myUserAccount) {
			const telephones = myUserAccount?.userAccountContactInformation?.telephones || [];
			form.reset({
				emailAddress: myUserAccount.emailAddress || '',
				extension: telephones?.length ? telephones[0]?.extension : '',
				firstName: myUserAccount.givenName || '',
				lastName: myUserAccount.familyName || '',
				phone: {
					code: '+1',
					flag: 'en-us',
				},
				phoneNumber: telephones?.length ? telephones[0]?.phoneNumber : '',
				publisherType: ['appPublisher'],
				requestDescription: '',
			});
		}
	}, [myUserAccount, form]);

	const userInfo = form.watch();

	const {data} = useListTypeDefinition('LT_PUBLISHER_TYPE');

	const submit = async (formValues: PublisherForm) => {
		const formData = {...formValues, intlCode: formValues?.phone?.code};

		delete formData.phone;

		try {
			await fetcher.post('/o/c/publisheraccountrequests', formData);

			setStep(PublisherGateStep.REQUESTED);
		}
		catch (error) {
			console.error(error);
		}
	};

	const StepsAccount = {
		[PublisherGateStep.FORM]: {
			component: (
				<PublisherGateForm
					form={form}
					listTypeDefinition={data}
					setStep={setStep}
				/>
			),
		},
		[PublisherGateStep.SUMMARY]: {
			component: (
				<PublisherGateSummary
					setStep={setStep}
					submit={form.handleSubmit(submit)}
				>
					<div className="mt-8">
						<PublisherSummaryContent
							title={i18n.translate('request-details')}
							userInfo={
								{
									...userInfo,
									phone: {
										code: userInfo?.phone?.code as string,
										flag: userInfo?.phone?.flag as string,
									},
									publisherType: userInfo.publisherType.map(
										(type) => {
											const entry = data?.listTypeEntries?.find(
												({key}) => type === key
											);
											if (entry) {
												return entry.name;
											}
											if (type === 'appPublisher') {
												return i18n.translate('app-publisher');
											}
											if (type === 'solutionPublisher') {
												return i18n.translate('solution-publisher');
											}
											return type;
										}
									),
								} as any
							}
						/>
					</div>
				</PublisherGateSummary>
			),
		},
		[PublisherGateStep.REQUESTED]: {
			component: <PubliserhRequestedCard />,
		},
	};

	return StepsAccount[step].component;
};

export default PublisherGateSteps;
