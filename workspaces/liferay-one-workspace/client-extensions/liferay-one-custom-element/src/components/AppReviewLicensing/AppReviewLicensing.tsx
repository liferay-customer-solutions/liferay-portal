/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {AppReviewProps} from '~/components/AppReview/AppReview';
import LicensingList from '~/components/AppReviewLicensingList/AppReviewLicensingList';
import AppReviewSection from '~/components/AppReviewSection/AppReviewSection';
import i18n from '~/i18n';

const AppReviewLicensing = ({
	context,
	editNavigate,
	required = false,
}: AppReviewProps) => {
	return (
		<AppReviewSection
			editNavigate={editNavigate}
			required={required}
			title={i18n.translate('licensing')}
		>
			<LicensingList context={context} />
		</AppReviewSection>
	);
};

export default AppReviewLicensing;
