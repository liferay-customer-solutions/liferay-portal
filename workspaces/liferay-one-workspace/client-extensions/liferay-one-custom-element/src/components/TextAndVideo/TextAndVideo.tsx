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
import VideoThumbnail from '~/components/VideoThumbnail/VideoThumbnail';
import i18n from '~/i18n';

import type {BlockTypeProps} from '~/components/Blocks/types';
import type {TextVideoBlock} from '~/context/SolutionContextProvider';

const TextAndVideo = ({
	block: {content},
	onChange,
}: BlockTypeProps<TextVideoBlock>) => (
	<>
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

		<div className="p-4">
			<Form.FormControl>
				<Form.Label htmlFor="videoUrl">
					{i18n.translate('video-url')}
				</Form.Label>

				<Form.Input
					name="videoUrl"
					onChange={(event) =>
						onChange({videoUrl: event.target.value})
					}
					placeholder="https://"
					type="text"
					value={content.videoUrl ?? ''}
				/>

				<Form.HelpMessage>
					{i18n.translate(
						'you-can-paste-links-directly-from-youtube'
					)}
				</Form.HelpMessage>
			</Form.FormControl>

			<Form.FormControl className="border d-flex flex-row mt-5 p-4 rounded">
				<VideoThumbnail videoURL={content.videoUrl ?? ''} />

				<Form.Input
					className="ml-3"
					name="videoDescription"
					onChange={(event) =>
						onChange({videoDescription: event.target.value})
					}
					placeholder={i18n.translate('video-description')}
					type="text"
					value={content.videoDescription ?? ''}
				/>
			</Form.FormControl>
		</div>
	</>
);

export default TextAndVideo;
