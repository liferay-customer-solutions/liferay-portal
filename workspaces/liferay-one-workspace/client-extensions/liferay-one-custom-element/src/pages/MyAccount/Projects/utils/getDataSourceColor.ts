/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const DEGREES_PER_TURN = 360;

const GOLDEN_ANGLE = 137.508;

const HASH_MODULUS = 9973;

const HASH_MULTIPLIER = 31;

const LIGHTNESSES = [42, 55, 68];

const SATURATION = 70;

export function getDataSourceColor(dataSourceId: string): string {
	let hash = 0;

	for (let index = 0; index < dataSourceId.length; index++) {
		hash =
			(hash * HASH_MULTIPLIER + dataSourceId.charCodeAt(index)) %
			HASH_MODULUS;
	}

	const hue = (hash * GOLDEN_ANGLE) % DEGREES_PER_TURN;

	const lightness = LIGHTNESSES[hash % LIGHTNESSES.length];

	return `hsl(${hue} ${SATURATION}% ${lightness}%)`;
}

export default getDataSourceColor;
