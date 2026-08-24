/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useProperties} from '~/context/PropertiesContext';
import i18n from '~/i18n';

const ContactSupport = () => {
	const {contactSupportURL} = useProperties();

	return (
		<p className="secondary-text">
			{i18n.translate('not-seeing-a-specific-project')}

			<a
				className="font-weight-semi-bold ml-1"
				href={contactSupportURL}
				rel="noopener noreferrer"
				target="_blank"
			>
				{i18n.translate('contact-support')}
			</a>
		</p>
	);
};

export default ContactSupport;
