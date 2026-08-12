/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import Build from '~/components/AppReviewBuild/AppReviewBuild';
import Categories from '~/components/AppReviewCategories/AppReviewCategories';
import Description from '~/components/AppReviewDescription/AppReviewDescription';
import Licensing from '~/components/AppReviewLicensing/AppReviewLicensing';
import Pricing from '~/components/AppReviewPricing/AppReviewPricing';
import Profile from '~/components/AppReviewProfile/AppReviewProfile';
import Storefront from '~/components/AppReviewStorefront/AppReviewStorefront';
import Support from '~/components/AppReviewSupport/AppReviewSupport';
import {NewAppInitialState} from '~/context/NewAppContextProvider';

import './AppReview.scss';

export type AppReviewProps = {
	children?: ReactNode;
	context: NewAppInitialState;
	editNavigate?: () => void;
	isLastSection?: boolean;
	required?: boolean;
};

const AppReview: React.FC<AppReviewProps> & {
	Build: React.FC<AppReviewProps>;
	Categories: React.FC<AppReviewProps>;
	Description: React.FC<AppReviewProps>;
	Licensing: React.FC<AppReviewProps>;
	Pricing: React.FC<AppReviewProps>;
	Profile: React.FC<AppReviewProps>;
	Storefront: React.FC<AppReviewProps>;
	Support: React.FC<AppReviewProps>;
} = ({children}) => <div>{children}</div>;

AppReview.Build = Build;
AppReview.Categories = Categories;
AppReview.Description = Description;
AppReview.Licensing = Licensing;
AppReview.Pricing = Pricing;
AppReview.Profile = Profile;
AppReview.Storefront = Storefront;
AppReview.Support = Support;

export default AppReview;
