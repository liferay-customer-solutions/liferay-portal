/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';
import {IBusinessEventVersion} from '~/pages/BusinessEvents/types';
import {getBusinessEventVersions} from '~/services/spring-boot/Jira';

export default function useGetBusinessEventVersions(
	id: string,
	projectERC: string
): {
	businessEventVersions: IBusinessEventVersion[];
	fetchBusinessEventVersions: () => Promise<void>;
	loading: boolean;
} {
	const [businessEventVersions, setBusinessEventVersions] = useState<
		IBusinessEventVersion[]
	>([]);

	const [loading, setLoading] = useState(true);

	const fetchBusinessEventVersions = useCallback(async () => {
		if (!id || !projectERC) {
			return;
		}

		setLoading(true);

		try {
			const response = await getBusinessEventVersions(id, projectERC);

			setBusinessEventVersions(
				(response.items || []) as IBusinessEventVersion[]
			);
		}
		catch (error) {
			setBusinessEventVersions([]);
		}
		finally {
			setLoading(false);
		}
	}, [id, projectERC]);

	useEffect(() => {
		fetchBusinessEventVersions();
	}, [fetchBusinessEventVersions]);

	return {businessEventVersions, fetchBusinessEventVersions, loading};
}
