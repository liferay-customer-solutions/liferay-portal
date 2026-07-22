/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	ProductSpecificationKey,
	getProductCategoriesByVocabularyName,
} from '~/utils/productUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import type {Product, ProductImages} from '~/types/product';

const SolutionImageTag = {
	DETAILS: 'solution-details',
	HEADER: 'solution-header',
} as const;

export type SolutionImage = {
	description?: string;
	fileName?: string;
	preview: string;
};

export type SolutionCompany = {
	description: string;
	email: string;
	phone: string;
	website: string;
};

export type SolutionBlock = {
	description: string;
	images?: SolutionImage[];
	title: string;
	type: string;
	videoDescription?: string;
	videoURL?: string;
};

export type SolutionDetail = {
	categories: string[];
	company?: SolutionCompany;
	contactEmail: string;
	description: string;
	details: SolutionBlock[];
	header: {
		description: string;
		images: SolutionImage[];
		title: string;
		videoDescription?: string;
		videoURL?: string;
	};
	name: string;
	tags: string[];
};

type RawSolutionBlock = {
	content?: {
		description?: string;
		files?: string[];
		title?: string;
		videoDescription?: string;
		videoUrl?: string;
	};
	type: string;
};

function toSolutionImage(image: ProductImages): SolutionImage {
	return {
		description: image.title?.en_US,
		fileName: image.title?.en_US,
		preview: image.src,
	};
}

export function parseSolutionDetail(product: Product): SolutionDetail {
	const images = product.images ?? [];

	const specifications = new Map<string, string>();

	for (const specification of product.productSpecifications ?? []) {
		specifications.set(
			specification.specificationKey,
			specification.value?.en_US ?? ''
		);
	}

	const headerVideoURL = specifications.get(
		ProductSpecificationKey.SOLUTION_HEADER_VIDEO_URL
	);

	const companyEmail = specifications.get(
		ProductSpecificationKey.SOLUTION_COMPANY_EMAIL
	);

	const detailImages = images.filter(({tags}) =>
		tags?.includes(SolutionImageTag.DETAILS)
	);

	const rawBlocks = safeJSONParse<RawSolutionBlock[]>(
		specifications.get(ProductSpecificationKey.SOLUTION_DETAILS_BLOCKS) ??
			null,
		[]
	);

	return {
		categories: getProductCategoriesByVocabularyName(
			product.categories,
			'marketplace-solution-category'
		),
		company: companyEmail
			? {
					description:
						specifications.get(
							ProductSpecificationKey.SOLUTION_COMPANY_DESCRIPTION
						) ?? '',
					email: companyEmail,
					phone:
						specifications.get(
							ProductSpecificationKey.SOLUTION_COMPANY_PHONE
						) ?? '',
					website:
						specifications.get(
							ProductSpecificationKey.SOLUTION_COMPANY_WEBSITE
						) ?? '',
				}
			: undefined,
		contactEmail:
			specifications.get(
				ProductSpecificationKey.SOLUTION_CONTACT_EMAIL
			) ?? '',
		description: product.description?.en_US ?? '',
		details: rawBlocks.map((block) => ({
			description: block.content?.description ?? '',
			images:
				block.type === 'text-images-block'
					? (block.content?.files ?? []).map(
							(externalReferenceCode) =>
								toSolutionImage(
									detailImages.find(
										(image) =>
											image.externalReferenceCode ===
											externalReferenceCode
									) ?? ({title: {}} as ProductImages)
								)
						)
					: undefined,
			title: block.content?.title ?? '',
			type: block.type,
			videoDescription: block.content?.videoDescription,
			videoURL: block.content?.videoUrl,
		})),
		header: {
			description:
				specifications.get(
					ProductSpecificationKey.SOLUTION_HEADER_DESCRIPTION
				) ?? '',
			images: headerVideoURL
				? []
				: images
						.filter(({tags}) =>
							tags?.includes(SolutionImageTag.HEADER)
						)
						.map(toSolutionImage),
			title:
				specifications.get(
					ProductSpecificationKey.SOLUTION_HEADER_TITLE
				) ?? '',
			videoDescription: specifications.get(
				ProductSpecificationKey.SOLUTION_HEADER_VIDEO_DESCRIPTION
			),
			videoURL: headerVideoURL,
		},
		name: product.name?.en_US ?? '',
		tags: getProductCategoriesByVocabularyName(
			product.categories,
			'marketplace-solution-tags'
		),
	};
}
