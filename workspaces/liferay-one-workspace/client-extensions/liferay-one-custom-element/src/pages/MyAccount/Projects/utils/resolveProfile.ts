/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

export function resolveProfile<T extends string>(
	value: string,
	profiles: readonly T[],
	fallback: T
): T {
	return (profiles as readonly string[]).includes(value)
		? (value as T)
		: fallback;
}

export default resolveProfile;
