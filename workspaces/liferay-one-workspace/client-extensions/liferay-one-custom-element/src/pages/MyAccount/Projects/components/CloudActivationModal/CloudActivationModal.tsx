/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';
import Button from '~/components/Button/Button';
import {Word, translate} from '~/i18n';

import CloudActivationForm from '../CloudActivationForm/CloudActivationForm';

import type {CloudActivationProfile} from '../../utils/cloudActivationFields';

const ALREADY_SUBMITTED_MESSAGE_KEY_BY_PROFILE: Record<
	CloudActivationProfile,
	Word
> = {
	'analytics-cloud':
		'another-user-already-submitted-the-analytics-cloud-activation-request',
	'paas': 'another-user-already-submitted-the-liferay-paas-activation-request',
	'saas': 'another-user-already-submitted-the-liferay-saas-activation-request',
};

const CONFIRMATION_MESSAGE_KEY_BY_PROFILE: Record<
	CloudActivationProfile,
	Word
> = {
	'analytics-cloud':
		'your-analytics-cloud-workspace-will-be-provisioned-in-1-2-business-days-an-email-will-be-sent-once-your-workspace-is-ready',
	'paas': 'your-liferay-paas-project-will-be-provisioned-in-2-3-business-days-at-that-time-liferay-paas-administrators-will-receive-several-onboarding-emails-giving-them-access-to-all-the-liferay-paas-environments-and-tools-included-in-your-subscription',
	'saas': 'your-liferay-saas-project-will-be-provisioned-within-5-business-days-an-email-will-be-sent-once-your-project-is-ready',
};

type Panel = 'already-submitted' | 'confirmation' | 'form';

type CloudActivationModalProps = {
	onClose: () => void;
	profile: CloudActivationProfile;
	projectExternalReferenceCode: string;
	projectName: string;
};

export default function CloudActivationModal({
	onClose,
	profile,
	projectExternalReferenceCode,
	projectName,
}: CloudActivationModalProps) {
	const [panel, setPanel] = useState<Panel>('form');

	if (panel === 'confirmation') {
		return (
			<div>
				<p className="fw-bold">
					{translate('thank-you-for-submitting-this-request')}
				</p>

				<p>{translate(CONFIRMATION_MESSAGE_KEY_BY_PROFILE[profile])}</p>

				<div className="d-flex justify-content-end mt-4">
					<Button onClick={onClose}>{translate('done')}</Button>
				</div>
			</div>
		);
	}

	if (panel === 'already-submitted') {
		return (
			<div>
				<p>
					{translate(
						ALREADY_SUBMITTED_MESSAGE_KEY_BY_PROFILE[profile]
					)}
				</p>

				<p>
					{translate(
						'return-to-the-product-activation-page-to-view-the-current-activation-status'
					)}
				</p>

				<div className="d-flex justify-content-end mt-4">
					<Button onClick={onClose}>{translate('done')}</Button>
				</div>
			</div>
		);
	}

	return (
		<CloudActivationForm
			onAlreadySubmitted={() => setPanel('already-submitted')}
			onSuccess={() => setPanel('confirmation')}
			profile={profile}
			projectExternalReferenceCode={projectExternalReferenceCode}
			projectName={projectName}
		/>
	);
}
