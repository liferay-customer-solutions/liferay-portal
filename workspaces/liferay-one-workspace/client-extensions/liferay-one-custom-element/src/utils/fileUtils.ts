/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export {downloadFile} from './downloadFile';

export function base64ToText(base64: string) {
	return base64.split(',').at(-1);
}

export function fileToBase64(
	file: File
): Promise<ArrayBuffer | null | string> {
	return new Promise((resolve, reject) => {
		const reader = new FileReader();

		reader.readAsDataURL(file);
		reader.onload = () => resolve(reader.result);
		reader.onerror = reject;
	});
}
