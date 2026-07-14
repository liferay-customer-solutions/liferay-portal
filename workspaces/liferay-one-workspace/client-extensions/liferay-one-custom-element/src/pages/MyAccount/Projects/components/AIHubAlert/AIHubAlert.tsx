/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import {useState} from 'react';
import {useProjectEnvironments} from '~/hooks/useProjectEnvironments';
import i18n from '~/i18n';
import {getIconSpriteMap} from '~/services/liferay/liferay';

export default function AIHubAlert() {
	const [showAlert, setShowAlert] = useState(true);

	const {environments} = useProjectEnvironments();

	const environment = environments.find((item) => item.type === 'AI Hub');

	if (!showAlert) {
		return null;
	}

	const alert =
		environment?.status === 'active'
			? {
					description: i18n.translate(
						'provisioning-is-complete-and-your-subscription-is-now-active-access-your-hub-via-the-url-below-to-start-using-your-monthly-token-allowance'
					),
					displayType: 'success' as const,
					title: i18n.translate('your-ai-hub-is-ready'),
				}
			: {
					description: i18n.translate(
						'weve-sent-the-order-form-to-your-email-via-docusign-please-review-sign-and-return-it-to-confirm-your-subscription-once-received-well-provision-your-ai-hub-and-notify-you-by-email'
					),
					displayType: 'info' as const,
					title: i18n.translate('awaiting-signature'),
				};

	return (
		<ClayAlert
			className="mb-0 mt-3"
			displayType={alert.displayType}
			onClose={() => setShowAlert(false)}
			spritemap={getIconSpriteMap()}
			title={alert.title}
		>
			{alert.description}
		</ClayAlert>
	);
}
