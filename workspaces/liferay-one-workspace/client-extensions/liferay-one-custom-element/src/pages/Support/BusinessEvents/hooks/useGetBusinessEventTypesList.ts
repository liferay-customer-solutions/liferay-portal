/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

import {IOption} from '../components/Select/Select';
import {getBusinessEventFieldOptions} from '../services/jira/Jira';
import {JSM_FIELDS} from '../utils/constants/jsmObjectTypes';

export default function useGetBusinessEventTypesList(): {
	businessEventTypesList: IOption[];
	error: boolean;
	loading: boolean;
} {
	const [businessEventTypesList, setBusinessEventTypesList] = useState<
		IOption[]
	>([]);
	const [error, setError] = useState(false);
	const [loading, setLoading] = useState(true);

	useEffect(() => {
		const fetchListTypeEntries = async () => {
			try {
				const response = await getBusinessEventFieldOptions(JSM_FIELDS.eventType);

				setBusinessEventTypesList(
					response
						.map((entry: any) => ({
							label: entry.label,
							value: entry.value,
						}))
						.sort((a: any, b: any) =>
							a.label.localeCompare(b.label)
						)
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

	return {businessEventTypesList, error, loading};
}
