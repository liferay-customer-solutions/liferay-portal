/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {zodResolver} from '@hookform/resolvers/zod';

import {useOneContext} from '~/context/OneContextProvider';
import useListTypeDefinition from '~/hooks/useListTypeDefinition';
import i18n from '~/i18n';
import publishingSchemas from '~/schema/publishingSchemas';
import fetcher from '~/services/fetcher/fetcher';
import {Liferay} from '~/services/liferay/liferay';
import BecomeAPublisherForm from './BecomeAPublisherForm';
import BecomeAPublisherSummary from './BecomeAPublisherSummary';
import PublisherRequestedCard from './PublisherRequestedCard';
import PublisherSummaryContent from './PublisherSummaryContent';

export type PublisherForm = z.infer<typeof publishingSchemas.becomePublisherForm>;

export enum BecomeAPublisherStep {
	FORM = 'form',
	SUMMARY = 'summary',
	REQUESTED = 'requested',
}

const BecomeAPublisherSteps = () => {
	const {myUserAccount} = useOneContext();
	const [step, setStep] = useState<BecomeAPublisherStep>(BecomeAPublisherStep.FORM);
	const userPhone =
		myUserAccount?.userAccountContactInformation?.telephones || [];

	const form = useForm<PublisherForm>({
		defaultValues: {
			emailAddress: myUserAccount ? myUserAccount?.emailAddress : '',
			extension: userPhone?.[0]?.extension ?? '',
			firstName: myUserAccount ? myUserAccount?.givenName : '',
			lastName: myUserAccount ? myUserAccount?.familyName : '',
			phone: {
				code: '+1',
				flag: 'en-us',
			},
			phoneNumber: userPhone?.[0]?.phoneNumber ?? '',
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
				extension: telephones?.[0]?.extension ?? '',
				firstName: myUserAccount.givenName || '',
				lastName: myUserAccount.familyName || '',
				phone: {
					code: '+1',
					flag: 'en-us',
				},
				phoneNumber: telephones?.[0]?.phoneNumber ?? '',
				publisherType: ['appPublisher'],
				requestDescription: '',
			});
		}
	}, [myUserAccount, form]);

	const userInfo = form.watch();

	const {data} = useListTypeDefinition('LT_PUBLISHER_TYPE');

	const submit = async (formValues: PublisherForm) => {
		const {phone, ...restFormValues} = formValues;
		const formData = {
			...restFormValues,
			intlCode: phone?.code,
		};

		try {
			await fetcher.post('/o/c/publisheraccountrequests', formData);

			setStep(BecomeAPublisherStep.REQUESTED);
		}
		catch (error) {
			console.error(error);

			Liferay.Util.openToast({
				message: i18n.translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
	};

	const StepsAccount = {
		[BecomeAPublisherStep.FORM]: {
			component: (
				<BecomeAPublisherForm
					form={form}
					listTypeDefinition={data}
					setStep={setStep}
				/>
			),
		},
		[BecomeAPublisherStep.SUMMARY]: {
			component: (
				<BecomeAPublisherSummary
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
										code: userInfo?.phone?.code ?? '',
										flag: userInfo?.phone?.flag ?? '',
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
				</BecomeAPublisherSummary>
			),
		},
		[BecomeAPublisherStep.REQUESTED]: {
			component: <PublisherRequestedCard />,
		},
	};

	return StepsAccount[step].component;
};

export default BecomeAPublisherSteps;
