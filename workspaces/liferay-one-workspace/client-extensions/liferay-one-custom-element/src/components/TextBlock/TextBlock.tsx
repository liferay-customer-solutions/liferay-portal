/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	MAX_DESCRIPTION_LENGTH,
	MAX_TITLE_LENGTH,
} from '~/components/Blocks/constants';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import RichText from '~/components/RichText/RichText';
import i18n from '~/i18n';

import type {BlockTypeProps} from '~/components/Blocks/types';
import type {TextBlock as TextBlockType} from '~/context/SolutionContextProvider';

const TextBlock = ({
	block: {content},
	onChange,
}: BlockTypeProps<TextBlockType>) => (
	<div className="p-4">
		<Form.FormControl>
			<Form.Label className="mt-2" htmlFor="title" required>
				{i18n.translate('title')}
			</Form.Label>

			<Form.Input
				maxLength={MAX_TITLE_LENGTH}
				name="title"
				onChange={(event) => onChange({title: event.target.value})}
				placeholder="Enter title"
				type="text"
				value={content.title ?? ''}
			/>
		</Form.FormControl>

		<Form.FormControl>
			<Form.Label className="mt-5" htmlFor="description" required>
				{i18n.translate('description')}
			</Form.Label>

			<RichText
				maxLength={MAX_DESCRIPTION_LENGTH}
				onChange={(description) => onChange({description})}
				placeholder={i18n.translate('insert-text-here')}
				value={content.description ?? ''}
			/>
		</Form.FormControl>
	</div>
);

export default TextBlock;
