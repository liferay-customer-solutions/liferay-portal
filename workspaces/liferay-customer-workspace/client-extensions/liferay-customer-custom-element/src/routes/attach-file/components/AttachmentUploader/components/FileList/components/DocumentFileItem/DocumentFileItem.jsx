/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from "@clayui/icon";
import CircularProgress from "../CircularProgress";
import i18n from "~/common/I18n";

const DocumentFileItem = ({
	isProcessing,
	onDelete,
	uploadedFile,
}) => {
	const uploadedError = (uploadedFile?.error).message;
	const showProgress =
		isProcessing && !uploadedFile?.uploaded && uploadedFile?.progress > 0;

	return (
		<div className="document-file-list-item-container"> 
			<div className="document-file-list-item-left-content"> 
				<div className="document-file-list-item-left-content-icon-container">
					{showProgress ? (
						<CircularProgress
							height={50}
							pathColor="#ffffff" 
							progress={uploadedFile?.progress}
							progressColor="#0B5FFF"
							width={50}
						/>
					) : (
						<ClayIcon
							aria-label="Document Icon"
							className="document-file-list-item-left-content-icon"
							symbol="document-default"
						/>
					)}
				</div>

				<div className="document-file-list-item-left-content-text-container">
					<span className="d-flex document-file-list-item-left-content-text-file-name">
						{uploadedFile?.fileName}
 
						{uploadedFile.uploaded &&
							uploadedFile.progress === 100 && (
								<span>
									<ClayIcon
										className="ml-2"
										color="green"
										fontSize={12}
										symbol="check"
									/>
								</span>
							)}

						{uploadedError && (
							<span
								aria-label={uploadedError}
								title={uploadedError}
							>
								<ClayIcon
									aria-label="Error Icon"
									className="ml-2"
									color="red"
									fontSize={12}
									symbol="times"
								/>
							</span>
						)}
					</span>

					<span className="document-file-list-item-left-content-text-file-size">
						{String(uploadedFile?.readableSize)}
					</span>
				</div>
			</div>

			{!isProcessing && (
				<button
					aria-label="Remove"
					className="document-file-list-item-button"
					onClick={() => onDelete(uploadedFile)}
				>
					{i18n.translate('remove')}
				</button>
			)}
		</div>
	);
}

export default DocumentFileItem;