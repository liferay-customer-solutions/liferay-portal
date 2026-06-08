/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

import {getFieldOptions} from '../services/jira/Jira';
import {IOption} from '../components/Select/Select';
import {JSM_FIELDS} from '../utils/constants/jsmObjectTypes';

export default function useGetUTCTimeZonesList(): {
	error: boolean;
	loading: boolean;
	utcTimeZonesList: IOption[];
} {
	const [error, setError] = useState(false);
	const [loading, setLoading] = useState(true);
	const [utcTimeZonesList, setUTCTimeZonesList] = useState<IOption[]>([]);

	useEffect(() => {
		const fetchListTypeEntries = async () => {
			try {
				const response = await getFieldOptions(JSM_FIELDS.timeZone);

				setUTCTimeZonesList(
					response.map((entry: any) => ({
						label: entry.name,
						value: entry.key,
					}))
				);
			}
			catch (error) {
				setError(true);
			}
			finally {
				setLoading(false);
			}
		};

		fetchListTypeEntries();
	}, []);

	return {error, loading, utcTimeZonesList};
}
