/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import type {ListTypeDefinition} from '~/types/listTypeDefinition';

export type PublisherTypeEntry = {
	key: string;
	name: string;
};

export const DEFAULT_PUBLISHER_TYPE_ENTRIES: PublisherTypeEntry[] = [
	{key: 'appPublisher', name: i18n.translate('app-publisher')},
	{key: 'solutionPublisher', name: i18n.translate('solution-publisher')},
];

export const PUBLISHER_TYPE_TOOLTIPS: Record<string, string> = {
	appPublisher: i18n.translate(
		'ability-to-publish-dxp-and-cloud-free-or-charged'
	),
	solutionPublisher: i18n.translate(
		'solutions-built-on-liferay-requires-existing-liferay-partnership'
	),
};

export function getPublisherTypeEntries(
	listTypeDefinition?: ListTypeDefinition
): PublisherTypeEntry[] {
	const entries = listTypeDefinition?.listTypeEntries;

	if (entries && !!entries.length) {
		return entries.map(({key, name}) => ({key, name}));
	}

	return DEFAULT_PUBLISHER_TYPE_ENTRIES;
}

export function getPublisherTypeNames(
	keys: string[],
	listTypeDefinition?: ListTypeDefinition
): string[] {
	const entries = getPublisherTypeEntries(listTypeDefinition);

	return keys.map((key) => {
		const entry =
			entries.find((entry) => entry.key === key) ??
			DEFAULT_PUBLISHER_TYPE_ENTRIES.find((entry) => entry.key === key);

		return entry?.name ?? key;
	});
}
