/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import DocumentFileItem from "./components/DocumentFileItem";

const FileList = ({
	isProcessing,
	onDelete,
	uploadedFiles,
}) => {
	return (
		<div className="file-list-container"> 
			<DocumentFileItem
				isProcessing={isProcessing}
				key={0}
				onDelete={onDelete}
				uploadedFile={uploadedFiles}
			/>
		</div>
	);
}

export default FileList;