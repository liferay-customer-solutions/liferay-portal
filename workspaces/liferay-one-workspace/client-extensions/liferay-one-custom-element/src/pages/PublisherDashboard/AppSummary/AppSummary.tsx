/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useNavigate} from 'react-router-dom';
import AppReview from '~/components/AppReview/AppReview';
import Button from '~/components/Button/Button';
import Page from '~/components/Page/Page';
import {useNewAppContext} from '~/context/NewAppContextProvider';
import i18n from '~/i18n';
import {
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
	getProductPageURL,
} from '~/utils/productUtils';

import '../PublisherDashboard.css';

import './AppSummary.css';

const STATUS_DOT_COLOR: Record<number, string> = {
	[ProductWorkflowStatusCode.APPROVED]: 'var(--color-success)',
	[ProductWorkflowStatusCode.DRAFT]: 'var(--color-neutral-5)',
	[ProductWorkflowStatusCode.PENDING]: 'var(--color-warning)',
};

export default function AppSummary() {
	const [context] = useNewAppContext();
	const navigate = useNavigate();

	const product = context._product;

	const productStatus = product?.productStatus as ProductWorkflowStatusCode;

	const isApproved = productStatus === ProductWorkflowStatusCode.APPROVED;

	const productPageURL = getProductPageURL(product?.urls);

	return (
		<Page
			description={i18n.translate(
				'review-the-information-published-for-this-app'
			)}
			pageRendererProps={{isLoading: context.loading}}
			rightButton={
				<Button
					displayType="secondary"
					onClick={() => navigate('/published-apps')}
				>
					{i18n.translate('back')}
				</Button>
			}
			title={context.profile.name}
		>
			<div className="app-review-container">
				<div className="align-items-center app-summary-status d-flex mb-4">
					<span
						className="app-summary-status-dot"
						style={{
							backgroundColor:
								STATUS_DOT_COLOR[productStatus] ??
								'var(--color-neutral-5)',
						}}
					/>

					<span className="font-weight-semi-bold">
						{ProductWorkflowStatusLabel[productStatus]}
					</span>

					{isApproved && productPageURL && (
						<a
							className="ml-4"
							href={productPageURL}
							rel="noopener noreferrer"
							target="_blank"
						>
							{i18n.translate('open-in-marketplace')}
						</a>
					)}
				</div>

				<div className="p-5 publisher-dashboard-card">
					<AppReview.Profile context={context} />

					<hr />

					<AppReview.Description context={context} />

					<AppReview.Categories context={context} />

					{!isApproved && <AppReview.Build context={context} />}

					<AppReview.Pricing context={context} />

					<AppReview.Licensing context={context} />

					<AppReview.Storefront context={context} />

					<AppReview.Support context={context} isLastSection />
				</div>
			</div>
		</Page>
	);
}
