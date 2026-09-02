/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

function getContentDispositionFileName(response: Response) {
	const contentDisposition = response.headers.get('content-disposition');

	if (!contentDisposition) {
		return '';
	}

	return (
		contentDisposition.split(';').find((n) => n.includes('filename=')) ?? ''
	)
		.replace('filename=', '')
		.replaceAll('"', '')
		.trim();
}

export function getFileNameExtension(fileName: string) {
	const extension = fileName.match(/\.[^./\\]+$/);

	return extension ? extension[0] : '';
}

export function downloadBlob(filename: string, blob: Blob) {
	const anchor = document.createElement('a');

	anchor.download = filename;
	anchor.href = URL.createObjectURL(blob);

	document.body.appendChild(anchor);

	anchor.click();
	anchor.remove();
}

export async function downloadFile(filename: string, response: Response) {
	const blob = await response.blob();

	downloadBlob(getContentDispositionFileName(response) || filename, blob);
}
