/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';

import {IOption} from '../components/Select/Select';
import {getProductVersions} from '../services/jira/Jira';
import sortLiferayVersions from '../utils/sortLiferayVersions';

export default function useGetLiferayVersions(): {
	error: boolean;
	loading: boolean;
	productVersions: IOption[];
} {
	const [error, setError] = useState(false);
	const [loading, setLoading] = useState(true);
	const [productVersions, setProductVersions] = useState<IOption[]>([]);

	useEffect(() => {
		const fetchLiferayVersions = async () => {
			try {
				const response = await getProductVersions();

				setProductVersions(
					sortLiferayVersions(
						response.map((entry: any) => ({
							key: entry.key,
							name: entry.name,
						}))
					).map(({key, name}) => ({label: name, value: key}))
				);
			}
			catch (error) {
				setError(true);
			}
			finally {
				setLoading(false);
			}
		};

		fetchLiferayVersions();
	}, []);

	return {
		error,
		loading,
		productVersions,
	};
}
