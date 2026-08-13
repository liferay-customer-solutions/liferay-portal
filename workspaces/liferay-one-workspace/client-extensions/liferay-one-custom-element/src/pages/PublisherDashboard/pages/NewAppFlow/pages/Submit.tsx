/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useNavigate} from 'react-router-dom';
import AppReview from '~/components/AppReview/AppReview';
import AppReviewSection from '~/components/AppReviewSection/AppReviewSection';
import {Section} from '~/components/Section/Section';
import {useNewAppContext} from '~/context/NewAppContextProvider';
import {usePublishMode} from '~/context/PublishModeContextProvider';
import i18n from '~/i18n';
import {ProductWorkflowStatusCode} from '~/utils/productUtils';

import {PublishMode} from '../constants';

import '../../../PublisherDashboard.css';

const Submit = () => {
	const [context] = useNewAppContext();
	const navigate = useNavigate();
	const mode = usePublishMode();

	const isNewVersion = mode === PublishMode.NEW_VERSION;

	const isEditingApp =
		context?._product &&
		context._product.productStatus === ProductWorkflowStatusCode.APPROVED;

	const editNavigate = (path: string) =>
		isNewVersion ? undefined : () => navigate(path);

	return (
		<div className="app-review-container">
			<Section
				disabled
				label={i18n.translate('app-submission')}
				tooltip={i18n.translate('more-info')}
				tooltipText={i18n.translate('more-info')}
			>
				<hr />
			</Section>

			<div className="p-5 publisher-dashboard-card">
				<AppReview.Profile context={context} />
				<hr />
				<AppReview.Description
					context={context}
					editNavigate={editNavigate('../profile')}
					required
				/>
				<AppReview.Categories
					context={context}
					editNavigate={editNavigate('../profile')}
					required
				/>
				{(!isEditingApp || isNewVersion) && (
					<AppReview.Build
						context={context}
						editNavigate={() => navigate('../build')}
						required
					/>
				)}
				{isNewVersion && (
					<AppReviewSection
						editNavigate={() => navigate('../version')}
						required
						title={i18n.translate('version')}
					>
						<p className="mb-1">{context.version.version}</p>

						<p className="text-secondary">
							{context.version.notes}
						</p>
					</AppReviewSection>
				)}
				<AppReview.Pricing
					context={context}
					editNavigate={editNavigate('../pricing')}
					required
				/>
				<AppReview.Licensing
					context={context}
					editNavigate={editNavigate('../licensing')}
					required
				/>
				<AppReview.Storefront
					context={context}
					editNavigate={editNavigate('../storefront')}
					required
				/>
				<AppReview.Support
					context={context}
					editNavigate={editNavigate('../support')}
					isLastSection
					required
				/>
			</div>
		</div>
	);
};

export default Submit;
