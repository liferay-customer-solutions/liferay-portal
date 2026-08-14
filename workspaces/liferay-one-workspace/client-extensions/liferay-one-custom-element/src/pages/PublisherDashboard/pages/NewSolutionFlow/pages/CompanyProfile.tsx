/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {MAX_DESCRIPTION_LENGTH} from '~/components/Blocks/constants';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import RichText from '~/components/RichText/RichText';
import {
	SolutionTypes,
	useSolutionContext,
} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';

const CompanyProfile = () => {
	const [
		{
			company: {description, email, phone, website},
		},
		dispatch,
	] = useSolutionContext();

	const onChange = (event: {target: {name: string; value: string}}) =>
		dispatch({
			payload: {[event.target.name]: event.target.value},
			type: SolutionTypes.SET_COMPANY,
		});

	return (
		<div className="mb-4">
			<h5>{i18n.translate('company-profile')}</h5>

			<hr />

			<Form.FormControl>
				<Form.Label className="mt-3" htmlFor="description" required>
					{i18n.translate('description')}
				</Form.Label>

				<RichText
					maxLength={MAX_DESCRIPTION_LENGTH}
					onChange={(value) =>
						dispatch({
							payload: {description: value},
							type: SolutionTypes.SET_COMPANY,
						})
					}
					placeholder={i18n.translate('insert-text-here')}
					value={description}
				/>
			</Form.FormControl>

			<Form.FormControl>
				<Form.Label className="mt-5" htmlFor="website" required>
					{i18n.translate('website')}
				</Form.Label>

				<Form.Input
					name="website"
					onChange={onChange}
					placeholder="http://www.yourdomain.com"
					type="text"
					value={website}
				/>
			</Form.FormControl>

			<Form.FormControl>
				<Form.Label className="mt-5" htmlFor="email" required>
					{i18n.translate('email')}
				</Form.Label>

				<Form.Input
					name="email"
					onChange={onChange}
					placeholder="name@yourdomain.com"
					type="email"
					value={email}
				/>
			</Form.FormControl>

			<Form.FormControl>
				<Form.Label className="mt-5" htmlFor="phone" required>
					{i18n.translate('phone')}
				</Form.Label>

				<Form.Input
					name="phone"
					onChange={onChange}
					placeholder="+1 (123) 456-7890"
					type="text"
					value={phone}
				/>
			</Form.FormControl>
		</div>
	);
};

export default CompanyProfile;
