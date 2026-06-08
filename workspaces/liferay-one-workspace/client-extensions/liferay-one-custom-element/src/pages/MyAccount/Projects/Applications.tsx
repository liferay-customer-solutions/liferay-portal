/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Page from '../../../components/Page';
import i18n from '../../../i18n';

export default function Applications() {
	return (
		<Page
			description={i18n.translate(
				'manage-the-applications-within-your-project'
			)}
			title={i18n.translate('applications')}
		>
			<></>
		</Page>
	);
}
