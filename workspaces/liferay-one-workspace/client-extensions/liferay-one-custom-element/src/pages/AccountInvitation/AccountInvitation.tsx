/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, useEffect, useState} from 'react';
import checkCircleIcon from '~/assets/icons/check_circle_icon.svg';
import hourglassIcon from '~/assets/icons/hourglass_icon.svg';
import timesCircleIcon from '~/assets/icons/times_circle_icon.svg';
import Button from '~/components/Button/Button';
import {Header} from '~/components/Header/Header';
import Loading from '~/components/Loading/Loading';
import {translate} from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
import Invitations from '~/services/spring-boot/Invitations';
import {getSiteURL} from '~/utils/siteUtils';

import './AccountInvitation.css';

import type {Word} from '~/i18n';

const InvitationStatuses = {
	ACCEPTED: 'accepted',
	ERROR: 'error',
	EXPIRED: 'expired',
	INVALID: 'invalid',
} as const;

type InvitationStatus =
	(typeof InvitationStatuses)[keyof typeof InvitationStatuses];

type InvitationState = {
	descriptionKey: Word;
	icon: string;
	titleKey: Word;
};

const invitationStates: Record<InvitationStatus, InvitationState> = {
	[InvitationStatuses.ACCEPTED]: {
		descriptionKey: 'you-have-joined-the-account-sign-in-to-get-started',
		icon: checkCircleIcon,
		titleKey: 'invitation-accepted',
	},
	[InvitationStatuses.ERROR]: {
		descriptionKey:
			'we-were-unable-to-complete-your-invitation-try-the-link-again-or-ask-an-account-administrator-for-a-new-one',
		icon: timesCircleIcon,
		titleKey: 'unable-to-accept-the-invitation',
	},
	[InvitationStatuses.EXPIRED]: {
		descriptionKey:
			'this-invitation-has-expired-ask-an-account-administrator-to-send-you-a-new-one',
		icon: hourglassIcon,
		titleKey: 'invitation-expired',
	},
	[InvitationStatuses.INVALID]: {
		descriptionKey:
			'this-invitation-link-is-not-valid-it-may-have-already-been-used-or-replaced-by-a-newer-invitation',
		icon: timesCircleIcon,
		titleKey: 'invitation-not-valid',
	},
};

function toInvitationStatus(status: string): InvitationStatus {
	if (status in invitationStates) {
		return status as InvitationStatus;
	}

	return InvitationStatuses.ERROR;
}

function useInvitationStatus() {
	const [status, setStatus] = useState<InvitationStatus>();

	useEffect(() => {
		const token = new URLSearchParams(window.location.search).get('token');

		if (!token) {
			setStatus(InvitationStatuses.INVALID);

			return;
		}

		const abortController = new AbortController();

		Invitations.getAccept(token, abortController.signal)
			.then(({status}) => setStatus(toInvitationStatus(status)))
			.catch((error) => {
				if (error.name !== 'AbortError') {
					setStatus(InvitationStatuses.ERROR);
				}
			});

		return () => abortController.abort();
	}, []);

	return status;
}

export default function AccountInvitation() {
	const status = useInvitationStatus();

	const invitationState = status ? invitationStates[status] : undefined;

	const dashboardURL = `${getSiteURL()}/my-account`;

	const action: ReactNode =
		status === InvitationStatuses.ACCEPTED &&
		!Liferay.ThemeDisplay.isSignedIn() ? (
			<Button
				onClick={() =>
					Liferay.Util.navigate(
						`/c/portal/login?redirect=${encodeURIComponent(
							dashboardURL
						)}`
					)
				}
			>
				{translate('sign-in')}
			</Button>
		) : (
			<Button onClick={() => Liferay.Util.navigate(dashboardURL)}>
				{translate('go-to-dashboard')}
			</Button>
		);

	return (
		<div className="account-invitation-page">
			<div className="account-invitation-page-content text-center">
				{invitationState ? (
					<>
						<Header
							description={translate(
								invitationState.descriptionKey
							)}
							icon={
								<img
									alt=""
									className="account-invitation-page-icon"
									draggable="false"
									src={invitationState.icon}
								/>
							}
							title={translate(invitationState.titleKey)}
						/>

						<div className="mt-4">{action}</div>
					</>
				) : (
					<Loading />
				)}
			</div>
		</div>
	);
}
