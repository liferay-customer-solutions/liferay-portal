/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';

import './CopyTokenField.css';

type CopyTokenFieldProps = {
	token: string;
};

export default function CopyTokenField({token}: CopyTokenFieldProps) {
	const handleCopy = () => {
		navigator.clipboard.writeText(token);

		Liferay.Util.openToast({
			message: i18n.sub('copied-x-to-the-clipboard', 'token'),
		});
	};

	return (
		<div className="copy-token-field">
			<ClayInput
				className="copy-token-field-input"
				readOnly
				value={token}
			/>

			<button
				aria-label={i18n.translate('copy')}
				className="copy-token-field-button"
				onClick={handleCopy}
				type="button"
			>
				<ClayIcon symbol="copy" />
			</button>
		</div>
	);
}
