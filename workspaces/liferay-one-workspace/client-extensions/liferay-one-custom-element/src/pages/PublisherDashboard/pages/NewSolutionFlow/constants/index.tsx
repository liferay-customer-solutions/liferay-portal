/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {SolutionInitialState} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';
import zodSchema from '~/schema/zodSchema';

import {PublishMode} from '../../NewAppFlow/constants';

import type {AppFlowItem} from '../../NewAppFlow/constants';

export const SOLUTIONS_EXIT_LINK = '/published-solutions';

export const BLOCK_TYPES = {
	TEXT: 'text-block',
	TEXT_IMAGES: 'text-images-block',
	TEXT_VIDEO: 'text-video-block',
} as const;

export const SOLUTION_FLOW_ITEMS: AppFlowItem<SolutionInitialState>[] = [
	{
		description: () =>
			'Review and accept the legal agreement between you and Liferay before proceeding. You are about to create a new solution submission.',
		label: i18n.translate('create'),
		modes: [PublishMode.CREATE],
		path: '',
		saveAsDraftRequired: false,
		title: () => 'Create new solution',
		visible: () => true,
	},
	{
		description: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Enter'} your solution details. This information will be used for submission, presentation, customer support, and search capabilities.`,
		label: i18n.translate('profile'),
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		parseSchema: (context: SolutionInitialState) =>
			zodSchema.solutionPublishing.profile.safeParse(context.profile),
		path: 'profile',
		saveAsDraftRequired: true,
		title: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Define'} the solution profile`,
		visible: () => true,
	},
	{
		description: () =>
			'Design the storefront for your solution. This will set the information displayed on the solution page. This section is dedicated to creating the solution header.',
		label: 'Solution Header',
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		parseSchema: (context: SolutionInitialState) =>
			zodSchema.solutionPublishing.header.safeParse(context.header),
		path: 'header',
		saveAsDraftRequired: false,
		title: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Customize'} solution header`,
		visible: () => true,
	},
	{
		description: () =>
			'Design the storefront for your solution. This will set the information displayed on the solution page. This section is dedicated to creating the solution detail content.',
		label: 'Solution Details',
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		parseSchema: (context: SolutionInitialState) =>
			zodSchema.solutionPublishing.details.safeParse(context.details),
		path: 'details',
		saveAsDraftRequired: false,
		title: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Customize'} storefront solution details`,
		visible: () => true,
	},
	{
		description: () =>
			'Define company profile information for your solution. This will inform users about your company on the storefront.',
		label: 'Company Profile',
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		parseSchema: (context: SolutionInitialState) =>
			zodSchema.solutionPublishing.company.safeParse(context.company),
		path: 'company',
		saveAsDraftRequired: false,
		title: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Provide'} company profile details`,
		visible: () => true,
	},
	{
		description: () =>
			'Define contact information for your solution. This will tell users how to reach you from the storefront.',
		label: 'Contact Us',
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		parseSchema: (context: SolutionInitialState) =>
			zodSchema.solutionPublishing.contactUs.safeParse(context.contactUs),
		path: 'contact',
		saveAsDraftRequired: false,
		title: (isEditing = false) =>
			`${isEditing ? 'Edit' : 'Provide'} contact us details`,
		visible: () => true,
	},
	{
		description: () =>
			'Please, review before submitting. Once sent, you will not be able to edit any information until this submission is completely reviewed by Liferay.',
		label: 'Submit',
		modes: [PublishMode.CREATE, PublishMode.EDIT],
		path: 'submit',
		saveAsDraftRequired: false,
		title: () => 'Review and submit solution',
		visible: () => true,
	},
];
