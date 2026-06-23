/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay/liferay';

import en_US from './en_US';
import es_ES from './es_ES';
import ja_JP from './ja_JP';
import pt_BR from './pt_BR';

export const languages = {
	en_US,
	es_ES,
	ja_JP,
	pt_BR,
};

export type Word = keyof typeof en_US;

export function translate(
	word: Word,
	languageId = Liferay.ThemeDisplay.getDefaultLanguageId()
): string {
	const languageProperties = (
		languages as unknown as Record<string, Partial<typeof en_US>>
	)[languageId];

	return languageProperties[word] || word;
}

export function sub(
	word: Word,
	words: Word[] | Word | string | string[]
): string {
	if (!Array.isArray(words)) {
		words = [words];
	}

	let translatedWord = translate(word);

	words.forEach((value, index) => {
		const translatedKey = translate(value as Word);
		const key = `{${index}}`;
		translatedWord = translatedWord.replace(key, translatedKey);
	});

	return translatedWord;
}

const i18n = {
	sub,
	translate,
};

export default i18n;
