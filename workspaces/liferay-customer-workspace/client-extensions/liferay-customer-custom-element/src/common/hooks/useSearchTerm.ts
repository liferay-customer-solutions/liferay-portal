/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';

type SearchTermHandler = (searchTerm: string) => void;

const useSearchTerm = (
	onSearch: SearchTermHandler
): [string, SearchTermHandler] => {
	const [lastSearchedTerm, setLastSearchedTerm] = useState<string>('');

	const handleSearchTermChange: SearchTermHandler = (
		searchTerm: string
	): void => {
		if (searchTerm !== lastSearchedTerm) {
			onSearch(searchTerm);
			setLastSearchedTerm(searchTerm);
		}
	};

	return [lastSearchedTerm, handleSearchTermChange];
};

export default useSearchTerm;
