/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import DOMPurify from 'dompurify';
import {useNavigate} from 'react-router-dom';
import AppReviewSection from '~/components/AppReviewSection/AppReviewSection';
import {Section} from '~/components/Section/Section';
import VideoThumbnail from '~/components/VideoThumbnail/VideoThumbnail';
import {useSolutionContext} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';

import {BLOCK_TYPES} from '../constants';

import '../../../PublisherDashboard.css';

import '../NewSolutionFlow.css';

import type {ContentBlock} from '~/context/SolutionContextProvider';

const SanitizedHTML = ({value}: {value: string}) => (
	<div dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(value)}} />
);

const BLOCK_LABELS = {
	[BLOCK_TYPES.TEXT]: i18n.translate('text-block'),
	[BLOCK_TYPES.TEXT_IMAGES]: i18n.translate('text-and-images-block'),
	[BLOCK_TYPES.TEXT_VIDEO]: i18n.translate('text-and-video-block'),
};

const Submit = () => {
	const [{company, contactUs, details, header, profile}] =
		useSolutionContext();
	const navigate = useNavigate();

	const editNavigate = (path: string) => () => navigate(path);

	const renderBlockMedia = (block: ContentBlock) => {
		if (block.type === BLOCK_TYPES.TEXT_IMAGES) {
			return block.content.files?.map((file) => (
				<p className="mb-1 text-secondary" key={file.id}>
					{file.imageDescription || file.fileName}
				</p>
			));
		}

		if (block.type === BLOCK_TYPES.TEXT_VIDEO) {
			return (
				<div className="d-flex">
					<VideoThumbnail videoURL={block.content.videoUrl} />

					<p className="ml-3 text-secondary">
						{block.content.videoDescription}
					</p>
				</div>
			);
		}

		return null;
	};

	return (
		<div className="app-review-container">
			<Section
				disabled
				label={i18n.translate('solution-submission')}
				tooltip={i18n.translate('more-info')}
				tooltipText={i18n.translate('more-info')}
			>
				<hr />
			</Section>

			<div className="p-5 publisher-dashboard-card">
				<div className="align-items-center d-flex mb-4">
					<img
						alt=""
						className="mr-4 new-solution-review-logo"
						src={profile.file?.preview}
					/>

					<h5 className="mb-0">{profile.name}</h5>
				</div>

				<hr />

				<AppReviewSection
					editNavigate={editNavigate('../profile')}
					required
					title={i18n.translate('description')}
				>
					<p className="mb-0">{profile.description}</p>
				</AppReviewSection>

				<AppReviewSection
					editNavigate={editNavigate('../profile')}
					required
					title={i18n.translate('categories')}
				>
					<div className="d-flex flex-wrap">
						{profile.categories.map((category) => (
							<span
								className="mr-3 new-solution-review-tag"
								key={category.value}
							>
								{category.label}
							</span>
						))}
					</div>
				</AppReviewSection>

				<AppReviewSection
					editNavigate={editNavigate('../profile')}
					required
					title={i18n.translate('tags')}
				>
					<div className="d-flex flex-wrap">
						{profile.tags.map((tag) => (
							<span
								className="mr-3 new-solution-review-tag"
								key={tag.value}
							>
								{tag.label}
							</span>
						))}
					</div>
				</AppReviewSection>

				<AppReviewSection
					editNavigate={editNavigate('../header')}
					required
					title={i18n.translate('solution-header')}
				>
					<p className="font-weight-semi-bold mb-1">{header.title}</p>

					<SanitizedHTML value={header.description} />

					{header.contentType.type === 'upload-images' &&
						header.contentType.content.headerImages.map((image) => (
							<p className="mb-1 text-secondary" key={image.id}>
								{image.imageDescription || image.fileName}
							</p>
						))}

					{header.contentType.type === 'embed-video-url' && (
						<div className="d-flex">
							<VideoThumbnail
								videoURL={
									header.contentType.content.headerVideoUrl
								}
							/>

							<p className="ml-3 text-secondary">
								{
									header.contentType.content
										.headerVideoDescription
								}
							</p>
						</div>
					)}
				</AppReviewSection>

				{!!details.length && (
					<AppReviewSection
						editNavigate={editNavigate('../details')}
						required
						title={i18n.translate('solution-details')}
					>
						{details.map((block, index) => (
							<div className="mb-4" key={index}>
								<p className="font-weight-semi-bold mb-1">
									{BLOCK_LABELS[block.type]}
								</p>

								<p className="mb-1">{block.content.title}</p>

								<SanitizedHTML
									value={block.content.description}
								/>

								{renderBlockMedia(block)}
							</div>
						))}
					</AppReviewSection>
				)}

				<AppReviewSection
					editNavigate={editNavigate('../company')}
					required
					title={i18n.translate('company-profile')}
				>
					<SanitizedHTML value={company.description} />

					<p className="mb-1">{company.website}</p>

					<p className="mb-1">{company.email}</p>

					<p className="mb-0">{company.phone}</p>
				</AppReviewSection>

				<AppReviewSection
					editNavigate={editNavigate('../contact')}
					isLastSection
					required
					title={i18n.translate('contact-us')}
				>
					<p className="mb-0">{contactUs}</p>
				</AppReviewSection>
			</div>
		</div>
	);
};

export default Submit;
