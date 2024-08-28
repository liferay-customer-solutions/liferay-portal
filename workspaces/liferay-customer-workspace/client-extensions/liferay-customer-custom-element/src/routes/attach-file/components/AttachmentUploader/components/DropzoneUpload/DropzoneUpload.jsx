/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classnames from 'classnames';
import Dropzone from 'react-dropzone';
import ClayIcon from '@clayui/icon';
import { Button } from '~/common/components';

const DropzoneUpload = ({ 
	buttonText,
	disabled = false,
	multiple,
	onHandleUpload,
	showDocumentIcon = true,
	title,
}) => {
	return (
		<Dropzone
			disabled={disabled}
			multiple={multiple}
			onDropAccepted={(file) => onHandleUpload(file)}
		>
			{({getInputProps, getRootProps, isDragActive, isDragReject}) => (
				<div
					className={classnames('dropzone-upload-container my-4', {
						'dropzone-upload-container-active': isDragActive,
						'dropzone-upload-container-disabled': disabled,
						'dropzone-upload-container-reject': isDragReject,
					})}
					{...getRootProps()}
				>
					{showDocumentIcon && (
						<div className="dropzone-upload-document-container">
							<ClayIcon
								aria-label="Document icon"
								className="dropzone-upload-document-icon"
								symbol="document-compressed"
							/>
						</div>
					)}

					<div className="dropzone-upload-text-container d-flex">
						<span className="dropzone-upload-text">{title}</span>

						<Button 
							aria-label="Select button"
							className="btn btn-outline-primary d-flex dropzone-upload-button ml-2"
						>
							<span>
								{buttonText}
							</span>
						</Button>
					</div>

					{!disabled && <input {...getInputProps()} />}
				</div>
			)}
		</Dropzone>
	);
}

export default DropzoneUpload;