import i18n from '~/common/I18n';
import AttachmentComment from './components/AttachmentComment';
import DropzoneUpload from './components/DropzoneUpload';
import { filesize } from 'filesize';
import { useState, useEffect } from 'react';
import FileList from './components/FileList';
import { Button as ClayButton } from '@clayui/core';

const AttachmentUploader = () => {
	const [hasFile, setHasFile] = useState(null);
	const [isCheckboxChecked, setIsCheckboxChecked] = useState(false);
	const [isProcessing, setIsProcessing] = useState(false);
	const [attachmentComment, setAttachmentComment] = useState('');
	const [ticketAttachmentId, setTicketAttachmentId] = useState(null);
	const [gcsSessionURL, setGcsSessionURL] = useState('');

	const zendeskTicketId = 378768;

	const handleUpload = (file) => {
		const newUploadedFile = {
			changed: true,
			error: false,
			file: file[0],
			fileName: file[0].name,
			progress: 0,
			readableSize: filesize(file[0].size),
			uploaded: true,
		};
		
		setHasFile(newUploadedFile);
		return newUploadedFile;
	};

	const initiateUpload = async (fileUploaded) => {
		try {
			const response = await Liferay.OAuth2Client.FromUserAgentApplication('liferay-customer-etc-spring-boot-oauth-application-user-agent').fetch("/ticket-attachments/initiate-upload", {
				body: JSON.stringify({
					zendeskTicketId,
					fileName: fileUploaded.fileName,
					fileSize: String(fileUploaded.file.size),
				}),
				method: 'POST',
			});
			
			if (!response.ok) {
				throw new Error('Failed to initiate upload: ' + response.statusText);
			}

			const responseText = await response.text();
			const responseJson = JSON.parse(responseText);

			setTicketAttachmentId(responseJson.ticketAttachmentId || null);
			setGcsSessionURL(responseJson.gcsSessionURL || '');
			
		} catch (error) {
			console.error('ErroInitUploader', error);
		}
	};

	const uploadFileToGcs = async () => {
		if (!gcsSessionURL || !hasFile) {
			return;
		}

		try {
			const response = await fetch(gcsSessionURL, {
				method: 'PUT',
				body: hasFile.file,
				headers: {
					'Content-Length': hasFile.file.size.toString(),
				},
			});

			if (!response.ok) {
				throw new Error('Failed to upload file to GCS: ' + response.statusText);
			}
		} catch (error) {
			console.error('Error uploading file to GCS:', error);
		}
	};

	const completeUpload = async () => {
		if (!ticketAttachmentId) {
			return;
		}

		try {
			const response = await Liferay.OAuth2Client.FromUserAgentApplication('liferay-customer-etc-spring-boot-oauth-application-user-agent').fetch(`/ticket-attachments/${ticketAttachmentId}/complete-upload`, {
				body: JSON.stringify({
					zendeskTicketCommentBody: attachmentComment,
				}),
				method: 'POST',
			});
			
			if (!response.ok) {
				throw new Error('Failed to complete upload: ' + response.statusText);
			}
			
			const responseText = await response.text();
			console.log('Complete Upload Response:', responseText);
		} catch (error) {
			console.error('ErrorCompUploader', error);
		}
	};

	useEffect(() => {
		if (gcsSessionURL && hasFile) {
			uploadFileToGcs();
		}
	}, [gcsSessionURL, hasFile]);

	useEffect(() => {
		if (ticketAttachmentId && gcsSessionURL) {
			completeUpload();
		}
	}, [ticketAttachmentId, gcsSessionURL]);

	const handleClickUpload = async () => {
		if (isProcessing) {
			return;
		}

		if (hasFile) {
			setIsProcessing(true);

			try {
				await initiateUpload(hasFile);
			} finally {
				setIsProcessing(false);
			}
		}
	};

	return (
		<div className='attachment-uploader'>
			<div className='d-flex mt-4 text-neutral-10'>
				<h2 className=''>{i18n.translate('attach-file-to-ticket')}{` #${zendeskTicketId}`}</h2>
			</div>

			<div className="mt-4">
				<div>
					<h5 className='text-neutral-9'>{i18n.translate('attachment')}</h5>

					<span className='text-neutral-8'>
						{i18n.translate('select-a-local-file-to-upload-only-one-file-can-be-attached-at-a-time')}
					</span>
				</div>

				{!hasFile && (
					<DropzoneUpload 
						buttonText={i18n.translate('select-a-file')}					
						multiple={false}
						onHandleUpload={handleUpload}
						title={i18n.translate('drag-and-drop-to-upload-or')}
					/>
				)}

				{!!hasFile && (
					<FileList
						onDelete={() => {
							setHasFile(null);
						}}
						type="document"
						uploadedFiles={hasFile}
						isProcessing={isProcessing}
					/>
				)}

				<AttachmentComment 
					attachmentComment={attachmentComment}
					isCheckboxChecked={isCheckboxChecked}
					setAttachmentComment={setAttachmentComment}
					setIsCheckboxChecked={setIsCheckboxChecked}
				/>

				<div className="d-flex my-4 px-4">
					<ClayButton
						aria-label="Close"
						displayType="secondary ml-auto mt-2"
					>
						{i18n.translate('close')}
					</ClayButton>

					<ClayButton 
						aria-label="Upload"
						disabled={!hasFile || !isCheckboxChecked || isProcessing}
						displayType="primary ml-3 mt-2"
						onClick={handleClickUpload}
					>
						{i18n.translate('upload')}
					</ClayButton>
				</div>
			</div>
		</div>
	);
};

export default AttachmentUploader;
