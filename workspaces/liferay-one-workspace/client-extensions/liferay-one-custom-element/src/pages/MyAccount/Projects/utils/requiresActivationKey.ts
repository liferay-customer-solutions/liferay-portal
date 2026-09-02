/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const LEGACY_VERSION_REGEX = /^(?:dxp\s+)?(\d+)\.(\d+|x)(?:\D|$)/i;

const SELF_SERVICE_MAJOR = 7;

const SELF_SERVICE_MINOR = 3;

export function requiresActivationKey(liferayVersion?: string): boolean {
	const match = LEGACY_VERSION_REGEX.exec((liferayVersion ?? '').trim());

	if (!match) {
		return false;
	}

	const major = Number(match[1]);

	if (major !== SELF_SERVICE_MAJOR) {
		return major < SELF_SERVICE_MAJOR;
	}

	const minor = Number(match[2]);

	return !Number.isNaN(minor) && minor < SELF_SERVICE_MINOR;
}

export default requiresActivationKey;
