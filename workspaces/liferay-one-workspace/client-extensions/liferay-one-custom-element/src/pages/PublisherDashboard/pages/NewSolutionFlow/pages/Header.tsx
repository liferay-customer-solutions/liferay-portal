/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayRadio, ClayRadioGroup} from '@clayui/form';
import ClayModal, {useModal} from '@clayui/modal';
import {filesize} from 'filesize';
import {
	MAX_DESCRIPTION_LENGTH,
	MAX_IMAGE_QUANTITY,
	MAX_SIZE_5MBS,
} from '~/components/Blocks/constants';
import {DropzoneUpload} from '~/components/DropzoneUpload/DropzoneUpload';
import {FileList, UploadedFile} from '~/components/FileList/FileList';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import RichText from '~/components/RichText/RichText';
import VideoThumbnail from '~/components/VideoThumbnail/VideoThumbnail';
import {
	SolutionTypes,
	useSolutionContext,
} from '~/context/SolutionContextProvider';
import {ACCEPT_FILE_TYPES} from '~/enums/File';
import i18n from '~/i18n';
import {getRandomID} from '~/utils/stringUtils';
import {swapElements} from '~/utils/swapElements';

const HEADER_CONTENT_TYPE = {
	EMBED_VIDEO_URL: 'embed-video-url',
	UPLOAD_IMAGES: 'upload-images',
} as const;

const MAX_HEADER_TITLE_LENGTH = 110;

const Header = () => {
	const {observer, onOpenChange, open} = useModal();

	const [
		{
			header: {contentType, description, title},
		},
		dispatch,
	] = useSolutionContext();

	const headerImages =
		contentType.type === HEADER_CONTENT_TYPE.UPLOAD_IMAGES
			? contentType.content.headerImages
			: [];

	const setHeaderImages = (images: UploadedFile[]) =>
		dispatch({
			payload: {
				contentType: {
					content: {headerImages: images},
					type: HEADER_CONTENT_TYPE.UPLOAD_IMAGES,
				},
			},
			type: SolutionTypes.SET_HEADER,
		});

	const onHandleUpload = (files: File[]) => {
		if (headerImages.length + files.length > MAX_IMAGE_QUANTITY) {
			return onOpenChange(true);
		}

		const uploadedFiles: UploadedFile[] = files.map((file) => ({
			changed: false,
			error: false,
			file,
			fileName: file.name,
			id: getRandomID(),
			preview: URL.createObjectURL(file),
			progress: 0,
			readableSize: filesize(file.size),
			uploaded: false,
		}));

		setHeaderImages([...headerImages, ...uploadedFiles]);
	};

	const handleDelete = async (id: string) => {
		dispatch({
			payload: id,
			type: SolutionTypes.SET_DELETE_IMAGE,
		});

		setHeaderImages(
			headerImages.filter((uploadedFile) => uploadedFile.id !== id)
		);
	};

	return (
		<div className="mb-4">
			<h5>{i18n.translate('solution-header')}</h5>

			<hr />

			<Form.FormControl>
				<Form.Label className="mt-2" htmlFor="title" required>
					{i18n.translate('title')}
				</Form.Label>

				<Form.Input
					maxLength={MAX_HEADER_TITLE_LENGTH}
					name="title"
					onChange={(event) =>
						dispatch({
							payload: {title: event.target.value},
							type: SolutionTypes.SET_HEADER,
						})
					}
					placeholder={i18n.translate('enter-title-header')}
					type="text"
					value={title}
				/>
			</Form.FormControl>

			<Form.FormControl>
				<Form.Label className="mt-5" htmlFor="description" required>
					{i18n.translate('description')}
				</Form.Label>

				<RichText
					maxLength={MAX_DESCRIPTION_LENGTH}
					onChange={(value) =>
						dispatch({
							payload: {description: value},
							type: SolutionTypes.SET_HEADER,
						})
					}
					placeholder={i18n.translate('insert-text-here')}
					value={description}
				/>
			</Form.FormControl>

			<Form.FormControl>
				<Form.Label className="mt-5" required>
					{i18n.translate('content-media-type')}
				</Form.Label>

				<ClayRadioGroup
					className="d-flex flex-column mt-1"
					onChange={(value) =>
						dispatch({
							payload: {
								contentType:
									value ===
									HEADER_CONTENT_TYPE.EMBED_VIDEO_URL
										? {
												content: {
													headerVideoDescription: '',
													headerVideoUrl: '',
												},
												type: HEADER_CONTENT_TYPE.EMBED_VIDEO_URL,
											}
										: {
												content: {headerImages: []},
												type: HEADER_CONTENT_TYPE.UPLOAD_IMAGES,
											},
							},
							type: SolutionTypes.SET_HEADER,
						})
					}
					value={contentType.type}
				>
					<ClayRadio
						label={i18n.translate('upload-images')}
						value={HEADER_CONTENT_TYPE.UPLOAD_IMAGES}
					/>

					<ClayRadio
						label={i18n.translate('embed-video-url')}
						value={HEADER_CONTENT_TYPE.EMBED_VIDEO_URL}
					/>
				</ClayRadioGroup>
			</Form.FormControl>

			{contentType.type === HEADER_CONTENT_TYPE.EMBED_VIDEO_URL && (
				<>
					<Form.FormControl>
						<Form.Label
							className="mt-5"
							htmlFor="headerVideoUrl"
							required
						>
							{i18n.translate('video-url')}
						</Form.Label>

						<Form.Input
							name="headerVideoUrl"
							onChange={(event) =>
								dispatch({
									payload: {
										contentType: {
											content: {
												...contentType.content,
												headerVideoUrl:
													event.target.value,
											},
											type: HEADER_CONTENT_TYPE.EMBED_VIDEO_URL,
										},
									},
									type: SolutionTypes.SET_HEADER,
								})
							}
							placeholder="https://"
							type="text"
							value={contentType.content.headerVideoUrl}
						/>

						<Form.HelpMessage>
							{i18n.translate(
								'you-can-paste-links-directly-from-youtube'
							)}
						</Form.HelpMessage>
					</Form.FormControl>

					<Form.FormControl className="border d-flex flex-row mt-5 p-4 rounded">
						<VideoThumbnail
							videoURL={contentType.content.headerVideoUrl}
						/>

						<Form.Input
							className="ml-3"
							name="headerVideoDescription"
							onChange={(event) =>
								dispatch({
									payload: {
										contentType: {
											content: {
												...contentType.content,
												headerVideoDescription:
													event.target.value,
											},
											type: HEADER_CONTENT_TYPE.EMBED_VIDEO_URL,
										},
									},
									type: SolutionTypes.SET_HEADER,
								})
							}
							placeholder={i18n.translate('video-description')}
							type="text"
							value={
								contentType.content.headerVideoDescription ?? ''
							}
						/>
					</Form.FormControl>
				</>
			)}

			{contentType.type === HEADER_CONTENT_TYPE.UPLOAD_IMAGES && (
				<Form.FormControl>
					<Form.Label className="mb-4 mt-5">
						{i18n.sub(
							'add-up-to-x-images',
							`${MAX_IMAGE_QUANTITY}`
						)}
					</Form.Label>

					{!!headerImages.length && (
						<FileList
							isProcessing={false}
							onArrowClick={(index, direction) => {
								const newIndex =
									direction === 'up' ? index - 1 : index + 1;

								const images = swapElements(
									headerImages,
									index,
									newIndex
								);

								images[index].changed = true;
								images[newIndex].changed = true;

								setHeaderImages(images);
							}}
							onChangeInput={setHeaderImages}
							onDelete={handleDelete}
							type="image"
							uploadedFiles={headerImages}
							uploadedImages={headerImages}
						/>
					)}

					<DropzoneUpload
						acceptFileTypes={ACCEPT_FILE_TYPES}
						buttonText={i18n.translate('select-a-file')}
						description={i18n.translate(
							'only-gif-jpg-jpeg-png-are-allowed-max-file-size-is-5mb'
						)}
						disabled={headerImages.length === MAX_IMAGE_QUANTITY}
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
			)}

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

export default Header;

export {HEADER_CONTENT_TYPE};
