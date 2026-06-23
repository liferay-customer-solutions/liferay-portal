/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayEmptyState from '@clayui/empty-state';
import React, {ReactNode} from 'react';
import i18n from '~/i18n';
import {Liferay} from '~/services/liferay/liferay';
export const States = {
	BLANK: '',

	EMPTY_SEARCH: `${Liferay.ThemeDisplay.getPathThemeImages()}/states/search_state.gif`,

	EMPTY_STATE: `${Liferay.ThemeDisplay.getPathThemeImages()}/states/empty_state.gif`,

	NO_ACCESS: `${Liferay.ThemeDisplay.getPathThemeImages()}/app_builder/illustration_locker.svg`,

	NOT_FOUND:
		'https://www.liferay.com/documents/10182/501717/404-Illustration-v2.svg',

	SUCCESS: `${Liferay.ThemeDisplay.getPathThemeImages()}/states/success_state.gif`,
};

export type EmptyStateProps = {
	children?: ReactNode;
	className?: string;
	description?: ReactNode | string;
	imgSrc?: string;
	title?: string;
	type?: keyof typeof States;
};

const EmptyState: React.FC<EmptyStateProps> = ({
	children,
	className,
	description,
	imgSrc,
	title,
	type,
}) => (
	<ClayEmptyState
		className={className}
		description={
			(description as string) ??
			i18n.translate('sorry-there-are-no-results-found')
		}
		imgSrc={imgSrc ?? (type ? States[type] : States.EMPTY_STATE)}
		title={title || i18n.translate('no-results-found')}
	>
		{children}
	</ClayEmptyState>
);

export default EmptyState;
