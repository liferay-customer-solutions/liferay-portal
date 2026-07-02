/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {zodResolver} from '@hookform/resolvers/zod';
import {useEffect, useState} from 'react';
import {useForm} from 'react-hook-form';
import {z} from 'zod';
import {useOneContext} from '~/context/OneContextProvider';
import useListTypeDefinition from '~/hooks/useListTypeDefinition';
import i18n from '~/i18n';
import publishingSchemas from '~/schema/publishingSchemas';
import fetcher from '~/services/fetcher/fetcher';
import {Liferay} from '~/services/liferay/liferay';

import {getPublisherTypeNames} from '../utils';
import PublisherRequestedCard from './components/PublisherRequestedCard';
import PublisherSummaryContent from './components/PublisherSummaryContent';
import RequestAccountForm from './components/RequestAccountForm';
import RequestAccountSummary from './components/RequestAccountSummary';

import type {UserAccount} from '~/types/accounts';

export type PublisherForm = z.infer<
	typeof publishingSchemas.becomePublisherForm
>;

export enum RequestAccountStep {
	FORM = 'form',
	SUMMARY = 'summary',
	REQUESTED = 'requested',
}

function buildDefaultValues(myUserAccount?: UserAccount): PublisherForm {
	const telephones =
		myUserAccount?.userAccountContactInformation?.telephones || [];

	return {
		emailAddress: myUserAccount?.emailAddress || '',
		extension: telephones?.[0]?.extension ?? '',
		firstName: myUserAccount?.givenName || '',
		lastName: myUserAccount?.familyName || '',
		phone: {
			code: '+1',
			flag: 'en-us',
		},
		phoneNumber: telephones?.[0]?.phoneNumber ?? '',
		publisherType: ['appPublisher'],
		requestDescription: '',
	};
}

const RequestAccount = () => {
	const {myUserAccount} = useOneContext();
	const [step, setStep] = useState<RequestAccountStep>(
		RequestAccountStep.FORM
	);

	const form = useForm<PublisherForm>({
		defaultValues: buildDefaultValues(myUserAccount),
		mode: 'onBlur',
		resolver: zodResolver(publishingSchemas.becomePublisherForm),
	});

	useEffect(() => {
		if (myUserAccount) {
			form.reset(buildDefaultValues(myUserAccount));
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

			setStep(RequestAccountStep.REQUESTED);
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
		[RequestAccountStep.FORM]: {
			component: (
				<RequestAccountForm
					form={form}
					listTypeDefinition={data}
					setStep={setStep}
				/>
			),
		},
		[RequestAccountStep.SUMMARY]: {
			component: (
				<RequestAccountSummary
					setStep={setStep}
					submit={form.handleSubmit(submit)}
				>
					<div className="mt-8">
						<PublisherSummaryContent
							title={i18n.translate('request-details')}
							userInfo={{
								...userInfo,
								phone: {
									code: userInfo?.phone?.code ?? '',
									flag: userInfo?.phone?.flag ?? '',
								},
								publisherType: getPublisherTypeNames(
									userInfo.publisherType,
									data
								),
							}}
						/>
					</div>
				</RequestAccountSummary>
			),
		},
		[RequestAccountStep.REQUESTED]: {
			component: <PublisherRequestedCard />,
		},
	};

	return StepsAccount[step].component;
};

export default RequestAccount;
