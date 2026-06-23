/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const UNITS = ['B', 'KB', 'MB', 'GB'];

export default function formatFileSize(fileSize?: string): string {
	const bytes = Number(fileSize);

	if (!fileSize || Number.isNaN(bytes) || bytes <= 0) {
		return '—';
	}

	let size = bytes;
	let unitIndex = 0;

	while (size >= 1024 && unitIndex < UNITS.length - 1) {
		size = size / 1024;
		unitIndex++;
	}

	const rounded = unitIndex === 0 ? size : Math.round(size * 10) / 10;

	return `${rounded} ${UNITS[unitIndex]}`;
}
