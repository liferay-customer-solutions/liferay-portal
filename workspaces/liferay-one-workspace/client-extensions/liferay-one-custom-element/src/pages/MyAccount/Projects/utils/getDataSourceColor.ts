/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const DEGREES_PER_TURN = 360;

const GOLDEN_ANGLE = 137.508;

const LIGHTNESS = 55;

const SATURATION = 70;

export function getDataSourceColor(index: number): string {
	const hue = (index * GOLDEN_ANGLE) % DEGREES_PER_TURN;

	return `hsl(${hue} ${SATURATION}% ${LIGHTNESS}%)`;
}

export default getDataSourceColor;
