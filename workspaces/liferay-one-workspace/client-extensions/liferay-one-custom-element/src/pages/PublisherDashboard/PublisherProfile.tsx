/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';
import {useNavigate} from 'react-router-dom';

import Button from '../../components/Button';
import {DetailedCard} from '../../components/DetailedCard/DetailedCard';
import Page from '../../components/Page';
import QATable, {Orientation} from '../../components/QATable';
import {ProductTypeVocabulary} from '../../enums/Product';
import {useFetch} from '../../hooks/useFetch';
import usePublisherCatalog from '../../hooks/usePublisherCatalog';
import usePublisherDetails from '../../hooks/usePublisherDetails';
import i18n, {Word} from '../../i18n';
import {
	PRODUCTS_RESOURCE,
	buildCatalogCategoryFilter,
} from './PublishedProductsListView';

const EMPTY_VALUE = '-';

function ProfileSection({title}: {title: Word}) {
	return (
		<p
			className="font-weight-semi-bold mb-2 mt-4 pb-2 text-secondary"
			style={{borderBottom: '1px solid var(--color-neutral-3)'}}
		>
			{i18n.translate(title)}
		</p>
	);
}

function useProductCount(
	catalogId: number | undefined,
	categoryVocabulary: ProductTypeVocabulary
) {
	const filter = catalogId
		? buildCatalogCategoryFilter(catalogId, categoryVocabulary)
		: undefined;

	const {data} = useFetch(catalogId ? PRODUCTS_RESOURCE : null, {
		params: {filter, pageSize: 1},
	});

	return data?.totalCount as number | undefined;
}

function value(content?: ReactNode) {
	return <p className="m-0">{content || EMPTY_VALUE}</p>;
}

export default function PublisherProfile() {
	const navigate = useNavigate();

	const {data: catalog, isLoading: isLoadingCatalog} = usePublisherCatalog();

	const catalogId = catalog?.id;

	const {isLoading: isLoadingDetails, publisherDetails} =
		usePublisherDetails(catalogId);

	const appsCount = useProductCount(catalogId, ProductTypeVocabulary.APP);
	const solutionsCount = useProductCount(
		catalogId,
		ProductTypeVocabulary.SOLUTION
	);

	const details = publisherDetails ?? {};

	return (
		<Page
			description={i18n.translate('manage-your-data-and-contacts')}
			pageRendererProps={{
				isLoading: isLoadingCatalog || isLoadingDetails,
			}}
			rightButton={
				<Button
					displayType="secondary"
					onClick={() => navigate('edit')}
				>
					{i18n.translate('edit')}
				</Button>
			}
			title={i18n.translate('publisher-profile')}
		>
			<div
				className="mt-4"
				style={{
					display: 'grid',
					gap: 'var(--spacer-4)',
					gridTemplateColumns: 'repeat(auto-fit, minmax(20rem, 1fr))',
				}}
			>
				<DetailedCard
					cardIconAltText={i18n.translate('public-information')}
					cardTitle={i18n.translate('public-information')}
					clayIcon="globe"
				>
					<div className="align-items-center d-flex mb-3 mt-3">
						{details.publisherProfileImageURL ? (
							<img
								alt={details.publisherName}
								src={details.publisherProfileImageURL}
								style={{
									borderRadius: '50%',
									height: '3rem',
									objectFit: 'cover',
									width: '3rem',
								}}
							/>
						) : (
							<span
								className="align-items-center bg-neutral-2 d-flex justify-content-center"
								style={{
									borderRadius: '50%',
									height: '3rem',
									width: '3rem',
								}}
							>
								{details.publisherName?.charAt(0) ?? 'P'}
							</span>
						)}

						<div className="ml-3">
							<p className="font-weight-bold h4 m-0">
								{details.publisherName || EMPTY_VALUE}
							</p>

							<p className="m-0 text-secondary">
								{i18n.sub('x-apps-x-solutions', [
									String(appsCount ?? 0),
									String(solutionsCount ?? 0),
								])}
							</p>
						</div>
					</div>

					<ProfileSection title="profile" />

					<QATable
						items={[
							{
								title: i18n.translate('company-description'),
								value: value(details.description),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>

					<ProfileSection title="contacts" />

					<QATable
						items={[
							{
								title: i18n.translate('company-email'),
								value: value(details.email),
							},
							{
								title: i18n.translate('support-email'),
								value: value(details.supportEmail),
							},
							{
								title: i18n.translate('sales-email'),
								value: value(details.salesEmail),
							},
							{
								title: i18n.translate('website'),
								value: details.websiteURL ? (
									<a
										href={details.websiteURL}
										rel="noopener noreferrer"
										target="_blank"
									>
										{details.websiteURL}
									</a>
								) : (
									value()
								),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>

					<ProfileSection title="address" />

					<QATable
						items={[
							{
								title: i18n.translate('company-address'),
								value: value(details.location),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>

				<DetailedCard
					cardIconAltText={i18n.translate('private-information')}
					cardTitle={i18n.translate('private-information')}
					clayIcon="password"
				>
					<ProfileSection title="profile" />

					<QATable
						items={[
							{
								title: i18n.translate('full-name'),
								value: value(details.fullName),
							},
							{
								title: i18n.translate('role'),
								value: value(details.role),
							},
							{
								title: i18n.translate('publisher-id'),
								value: value(details.id),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>

					<ProfileSection title="contacts" />

					<QATable
						items={[
							{
								title: i18n.translate('email'),
								value: value(details.privateEmail),
							},
							{
								title: i18n.translate('phone'),
								value: value(details.phone),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>

					<ProfileSection title="reimbursement" />

					<QATable
						items={[
							{
								title: i18n.translate('paypal-account'),
								value: value(details.paypalAccount),
							},
						]}
						orientation={Orientation.VERTICAL}
					/>
				</DetailedCard>
			</div>
		</Page>
	);
}
