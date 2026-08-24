/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {useMemo} from 'react';
import Loading from '~/components/Loading/Loading';
import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import {useProperties} from '~/context/PropertiesContext';
import i18n from '~/i18n';

import WizardFooter from '../WizardFooter/WizardFooter';

import type {UseFormReturn} from 'react-hook-form';

import type {InstallAppForm} from '../CloudAppInstall';

type InstallationProps = {
	form: UseFormReturn<InstallAppForm>;
	onClickBackToActivation: () => void;
};

const MARKETPLACE_ADMIN_EMAIL = 'marketplace-admin@liferay.com';

const getStatuses = () => ({
	failed: {
		bodyMessage: (
			<span
				dangerouslySetInnerHTML={{
					__html: i18n.sub(
						'we-could-not-install-your-app-please-try-again-if-the-problem-continues-contact-x-for-assistance',
						[
							`<a href="mailto:${MARKETPLACE_ADMIN_EMAIL}">${MARKETPLACE_ADMIN_EMAIL}</a>`,
						]
					),
				}}
			/>
		),
		icon: (
			<ClayIcon
				className="text-danger"
				fontSize="4rem"
				symbol="times-circle-full"
			/>
		),
		title: i18n.translate('installation-failed'),
	},
	loading: {
		bodyMessage: i18n.translate(
			'the-installation-process-is-ongoing-and-may-take-some-time-navigating-to-other-sections-will-not-cancel-the-process'
		),
		icon: <Loading displayType="primary" shape="squares" size="lg" />,
		title: i18n.translate('installation-in-progress'),
	},
	success: {
		bodyMessage: i18n.translate(
			'you-can-view-your-app-in-cloud-console-or-go-back-to-my-apps'
		),
		icon: (
			<ClayIcon
				className="text-success"
				fontSize="4rem"
				symbol="check-circle-full"
			/>
		),
		title: i18n.translate('installation-success'),
	},
});

const Installation = ({form, onClickBackToActivation}: InstallationProps) => {
	const {cloudConsoleURL} = useProperties();

	const {
		formState: {isSubmitSuccessful, isSubmitted, isSubmitting},
		watch,
	} = form;

	const environment = watch('environment');

	const isLoading = isSubmitting || !isSubmitted;

	const status = useMemo(() => {
		const statuses = getStatuses();

		if (isLoading) {
			return statuses.loading;
		}

		if (isSubmitSuccessful) {
			return statuses.success;
		}

		return statuses.failed;
	}, [isLoading, isSubmitSuccessful]);

	return (
		<ProductPurchase.Shell
			className="align-items-center d-flex flex-column mt-5"
			title={status.title}
		>
			{status.icon}

			<span className="col-7 mt-6 text-center">{status.bodyMessage}</span>

			<WizardFooter
				backButtonProps={{
					children: i18n.translate('go-to-app-provisioning'),
					onClick: onClickBackToActivation,
				}}
				continueButtonProps={{
					children: i18n.translate('view-app-in-cloud'),
					className: classNames({
						'd-none': !isSubmitSuccessful,
					}),
					onClick: () =>
						window.open(
							`${cloudConsoleURL}/projects/${environment?.projectId}/services`,
							'_blank',
							'noopener,noreferrer'
						),
				}}
			/>
		</ProductPurchase.Shell>
	);
};

export default Installation;
