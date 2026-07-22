/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayAlert from '@clayui/alert';
import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayLabel from '@clayui/label';
import DOMPurify from 'dompurify';
import {ComponentProps, ReactNode} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {PageRenderer} from '~/components/Page/Page';
import useAdminSolution from '~/hooks/useAdminSolution';
import i18n from '~/i18n';
import {
	ProductSpecificationKey,
	ProductWorkflowDisplayType,
	ProductWorkflowStatusCode,
	ProductWorkflowStatusLabel,
} from '~/utils/productUtils';

import {SolutionImage, parseSolutionDetail} from './parseSolutionDetail';

import type {Word} from '~/i18n';
import type {Product} from '~/types/product';

const PROTOCOLS = ['http://', 'https://'];

const SUPPORT_LINK_PREFIX = {
	email: 'mailto:',
	phone: 'tel:',
	url: '',
};

type SectionProps = {
	children: ReactNode;
	isLastSection?: boolean;
	title: string;
};

function Section({children, isLastSection = false, title}: SectionProps) {
	return (
		<>
			<div className="mb-4">
				<h3 className="mb-3">{title}</h3>

				{children}
			</div>

			{!isLastSection && <hr />}
		</>
	);
}

type ParagraphProps = {
	children: ReactNode;
	title?: string;
};

function Paragraph({children, title}: ParagraphProps) {
	return (
		<div className="mb-4">
			{title && <span className="d-block h5 mb-2">{title}</span>}

			{children}
		</div>
	);
}

function Html({value}: {value: string}) {
	return (
		<div dangerouslySetInnerHTML={{__html: DOMPurify.sanitize(value)}} />
	);
}

function ImageInfo({icon, image}: {icon: string; image: SolutionImage}) {
	return (
		<div className="d-flex mt-3">
			<img
				className="mr-3"
				src={image.preview}
				style={{maxWidth: '12rem'}}
			/>

			<div>
				<ClayIcon className="mb-2" symbol={icon} />

				<div className="font-weight-semi-bold">{image.fileName}</div>

				<p className="mb-0">{image.description}</p>
			</div>
		</div>
	);
}

type VideoLinkProps = {
	videoDescription?: string;
	videoURL: string;
};

function VideoLink({videoDescription, videoURL}: VideoLinkProps) {
	return (
		<div className="d-flex mt-3">
			<ClayIcon className="mr-3" symbol="document-multimedia" />

			<div>
				<a href={videoURL} rel="noopener noreferrer" target="_blank">
					{videoURL}
				</a>

				{videoDescription && <p className="mb-0">{videoDescription}</p>}
			</div>
		</div>
	);
}

type SupportLinkProps = {
	href: string;
	label: string;
	symbol: string;
	type: 'email' | 'phone' | 'url';
};

function SupportLink({href, label, symbol, type}: SupportLinkProps) {
	function getHref() {
		if (type === 'url') {
			return PROTOCOLS.some((protocol) => href.includes(protocol))
				? href
				: `https://${href}`;
		}

		return `${SUPPORT_LINK_PREFIX[type]}${href}`;
	}

	return (
		<div className="align-items-center border d-flex mt-3 p-4 rounded-lg">
			<ClayIcon className="mr-3" symbol={symbol} />

			<div className="d-flex flex-column">
				<span className="font-weight-semi-bold">{label}</span>

				<a href={getHref()} rel="noopener noreferrer" target="_blank">
					{href}
				</a>
			</div>
		</div>
	);
}

function SolutionDetailContent({product}: {product: Product}) {
	const navigate = useNavigate();

	const {code} = product.workflowStatusInfo;

	const solution = parseSolutionDetail(product);

	const version = product.productSpecifications.find(
		({specificationKey}) =>
			specificationKey === ProductSpecificationKey.APP_VERSION
	)?.value?.en_US;

	return (
		<div className="w-100">
			<ClayButton
				className="align-items-center d-flex mb-4"
				displayType="unstyled"
				onClick={() => navigate('/mp-solutions')}
			>
				<ClayIcon className="mr-2" symbol="order-arrow-left" />

				<span className="h5 mb-0">
					{i18n.translate('back-to-solutions')}
				</span>
			</ClayButton>

			{code === ProductWorkflowStatusCode.PENDING && (
				<ClayAlert displayType="info">
					{i18n.translate(
						'this-submission-is-currently-under-review-by-liferay-once-the-process-is-complete-it-will-be-published-on-the-marketplace-in-the-meantime-no-information-or-data-from-this-app-submission-can-be-updated'
					)}
				</ClayAlert>
			)}

			<div className="align-items-center d-flex mb-4">
				<img
					alt={solution.name}
					className="mr-3"
					src={product.thumbnail}
					style={{height: '4rem', width: '4rem'}}
				/>

				<div>
					<h2 className="mb-1 text-truncate" title={solution.name}>
						{solution.name}
					</h2>

					<div className="align-items-center d-flex">
						{version && (
							<span className="mr-3 text-secondary">
								{version}
							</span>
						)}

						<ClayLabel
							displayType={
								ProductWorkflowDisplayType[
									code as keyof typeof ProductWorkflowDisplayType
								] as ComponentProps<
									typeof ClayLabel
								>['displayType']
							}
						>
							{
								ProductWorkflowStatusLabel[
									code as keyof typeof ProductWorkflowStatusLabel
								]
							}
						</ClayLabel>
					</div>
				</div>
			</div>

			<div className="border p-5 rounded-lg">
				<Section title={i18n.translate('description')}>
					<Html value={solution.description} />

					{!!solution.categories.length && (
						<Paragraph title={i18n.translate('categories')}>
							<div className="d-flex flex-wrap">
								{solution.categories.map((category, index) => (
									<ClayLabel className="mr-2" key={index}>
										{category}
									</ClayLabel>
								))}
							</div>
						</Paragraph>
					)}

					{!!solution.tags.length && (
						<Paragraph title={i18n.translate('tags')}>
							<div className="d-flex flex-wrap">
								{solution.tags.map((tag, index) => (
									<ClayLabel className="mr-2" key={index}>
										{tag}
									</ClayLabel>
								))}
							</div>
						</Paragraph>
					)}
				</Section>

				<Section title={i18n.translate('header')}>
					<Paragraph title={solution.header.title}>
						<Html value={solution.header.description} />
					</Paragraph>

					{solution.header.images.map((image, index) => (
						<ImageInfo
							icon="document-image"
							image={image}
							key={index}
						/>
					))}

					{solution.header.videoURL && (
						<VideoLink
							videoDescription={solution.header.videoDescription}
							videoURL={solution.header.videoURL}
						/>
					)}
				</Section>

				{!!solution.details.length && (
					<Section title={i18n.translate('solution-details')}>
						{solution.details.map((block, index) => (
							<div
								className="border mb-3 overflow-hidden rounded-lg"
								key={index}
							>
								<div className="bg-light px-4 py-3">
									<strong>
										{i18n.translate(block.type as Word)}
									</strong>
								</div>

								<div className="p-4">
									<Paragraph title={i18n.translate('title')}>
										{i18n.translate(block.title as Word)}
									</Paragraph>

									<Paragraph
										title={i18n.translate('description')}
									>
										<Html value={block.description} />
									</Paragraph>

									{block.images?.map((image, imageIndex) => (
										<ImageInfo
											icon="document-image"
											image={image}
											key={imageIndex}
										/>
									))}

									{block.videoURL && (
										<VideoLink
											videoDescription={
												block.videoDescription
											}
											videoURL={block.videoURL}
										/>
									)}
								</div>
							</div>
						))}
					</Section>
				)}

				{solution.company && (
					<Section title={i18n.translate('company-profile')}>
						<Html value={solution.company.description} />

						{solution.company.website && (
							<SupportLink
								href={solution.company.website}
								label={i18n.translate('publisher-website-url')}
								symbol="globe"
								type="url"
							/>
						)}

						{solution.company.email && (
							<SupportLink
								href={solution.company.email}
								label={i18n.translate('email')}
								symbol="envelope-closed"
								type="email"
							/>
						)}

						{solution.company.phone && (
							<SupportLink
								href={solution.company.phone}
								label={i18n.translate('phone')}
								symbol="phone"
								type="phone"
							/>
						)}
					</Section>
				)}

				<Section isLastSection title={i18n.translate('contact-us')}>
					{solution.contactEmail && (
						<SupportLink
							href={solution.contactEmail}
							label={i18n.translate('email')}
							symbol="envelope-closed"
							type="email"
						/>
					)}
				</Section>
			</div>
		</div>
	);
}

export default function SolutionDetail() {
	const {productId} = useParams();

	const {data: product, error, isLoading} = useAdminSolution(productId!);

	return (
		<PageRenderer error={error} isLoading={isLoading}>
			{product && <SolutionDetailContent product={product} />}
		</PageRenderer>
	);
}
