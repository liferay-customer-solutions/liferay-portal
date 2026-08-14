/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Form from '~/components/MarketplaceForm/MarketplaceForm';
import {
	SolutionTypes,
	useSolutionContext,
} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';

const ContactUs = () => {
	const [{contactUs}, dispatch] = useSolutionContext();

	return (
		<div className="mb-4">
			<h5>{i18n.translate('contact-us')}</h5>

			<hr />

			<Form.FormControl>
				<Form.Label
					className="mt-3"
					htmlFor="email"
					info={i18n.translate('email')}
					required
				>
					{i18n.translate('email')}
				</Form.Label>

				<Form.Input
					name="email"
					onChange={(event) =>
						dispatch({
							payload: event.target.value,
							type: SolutionTypes.SET_CONTACT_US,
						})
					}
					placeholder="name@yourdomain.com"
					type="email"
					value={contactUs}
				/>
			</Form.FormControl>
		</div>
	);
};

export default ContactUs;
