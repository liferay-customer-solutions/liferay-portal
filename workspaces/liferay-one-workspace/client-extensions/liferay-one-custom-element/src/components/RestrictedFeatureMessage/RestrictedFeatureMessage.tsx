/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Text} from '@clayui/core';
import ClayIcon from '@clayui/icon';
import {translate} from '~/i18n';

import './RestrictedFeatureMessage.css';

type RestrictedFeatureMessageProps = {
	message?: string;
};

export default function RestrictedFeatureMessage({
	message = translate('this-feature-is-not-included-in-your-current-plan'),
}: RestrictedFeatureMessageProps) {
	return (
		<div className="mt-3 restricted-feature-message">
			<ClayIcon
				className="restricted-feature-message-icon"
				symbol="lock"
			/>

			<Text size={5} weight="semi-bold">
				{message}
			</Text>
		</div>
	);
}
