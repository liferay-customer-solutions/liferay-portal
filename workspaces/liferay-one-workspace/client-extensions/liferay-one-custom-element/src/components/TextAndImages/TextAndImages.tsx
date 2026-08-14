/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayModal, {useModal} from '@clayui/modal';
import {filesize} from 'filesize';
import {
	MAX_DESCRIPTION_LENGTH,
	MAX_IMAGE_QUANTITY,
	MAX_SIZE_5MBS,
	MAX_TITLE_LENGTH,
} from '~/components/Blocks/constants';
import {DropzoneUpload} from '~/components/DropzoneUpload/DropzoneUpload';
import {FileList, UploadedFile} from '~/components/FileList/FileList';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import RichText from '~/components/RichText/RichText';
import {ACCEPT_FILE_TYPES} from '~/enums/File';
import i18n from '~/i18n';
import {getRandomID} from '~/utils/stringUtils';
import {swapElements} from '~/utils/swapElements';

import type {BlockTypeProps} from '~/components/Blocks/types';
import type {TextImageBlock} from '~/context/SolutionContextProvider';

const TextAndImages = ({
	block: {content},
	onChange,
	onDeleteImage,
}: BlockTypeProps<TextImageBlock>) => {
	const {observer, onOpenChange, open} = useModal();

	const onHandleUpload = (files: File[]) => {
		if ((content.files?.length ?? 0) + files.length > MAX_IMAGE_QUANTITY) {
			return onOpenChange(true);
		}

		const uploadedFiles: UploadedFile[] = files.map((file, index) => ({
			changed: false,
			error: false,
			file,
			fileName: file.name,
			id: getRandomID(),
			index,
			preview: URL.createObjectURL(file),
			progress: 0,
			readableSize: filesize(file.size),
			uploaded: false,
		}));

		onChange({
			files: content.files
				? [...content.files, ...uploadedFiles]
				: uploadedFiles,
		});
	};

	return (
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

			<Form.FormControl>
				<Form.Label className="mt-5">
					{i18n.sub('add-up-to-x-images', `${MAX_IMAGE_QUANTITY}`)}
				</Form.Label>

				{!!content.files?.length && (
					<FileList
						isProcessing={false}
						onArrowClick={(index, direction) => {
							const newIndex =
								direction === 'up' ? index - 1 : index + 1;

							const files = swapElements(
								content.files,
								index,
								newIndex
							);

							files[index].changed = true;
							files[newIndex].changed = true;

							onChange({files});
						}}
						onChangeInput={(files) => onChange({files})}
						onDelete={(id) => {
							onDeleteImage(id);

							onChange({
								files: content.files.filter(
									(uploadedFile) => uploadedFile.id !== id
								),
							});
						}}
						type="image"
						uploadedFiles={content.files}
						uploadedImages={content.files}
					/>
				)}

				<DropzoneUpload
					acceptFileTypes={ACCEPT_FILE_TYPES}
					buttonText={i18n.translate('select-a-file')}
					description={i18n.translate(
						'only-gif-jpg-jpeg-png-are-allowed-max-file-size-is-5mb'
					)}
					disabled={content.files?.length === MAX_IMAGE_QUANTITY}
					maxFiles={MAX_IMAGE_QUANTITY}
					maxSize={MAX_SIZE_5MBS}
					multiple
					onDropRejected={(fileList) => {
						if (fileList.length > MAX_IMAGE_QUANTITY) {
							onOpenChange(true);
						}
					}}
					onHandleUpload={onHandleUpload}
					title={i18n.translate('drag-and-drop-to-upload-or')}
				/>
			</Form.FormControl>

			{open && (
				<ClayModal center observer={observer} status="info">
					<ClayModal.Header>
						{i18n.translate('maximum-number-of-uploads-reached')}
					</ClayModal.Header>

					<ClayModal.Body className="pb-8">
						{i18n.sub(
							'you-cannot-upload-more-than-x-files',
							`${MAX_IMAGE_QUANTITY}`
						)}
					</ClayModal.Body>
				</ClayModal>
			)}
		</div>
	);
};

export default TextAndImages;
