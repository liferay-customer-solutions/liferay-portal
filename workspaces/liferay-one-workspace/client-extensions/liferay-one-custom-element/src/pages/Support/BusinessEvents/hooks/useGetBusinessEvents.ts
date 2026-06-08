/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';

import {getBusinessEvents} from '../services/jira/Jira';
import {IBusinessEvent} from '../types';

export default function useGetBusinessEvents(accountKey: string): {
	businessEvents: IBusinessEvent[];
	fetchBusinessEvents: () => Promise<void>;
	loading: boolean;
} {
	const [businessEvents, setBusinessEvents] = useState<IBusinessEvent[]>([]);

	const [loading, setLoading] = useState(true);

	const fetchBusinessEvents = useCallback(async () => {
		if (!accountKey) {
			return;
		}

		try {
			const businessEventsResponse = await getBusinessEvents(accountKey);

			const items = (businessEventsResponse.items ||
				[]) as IBusinessEvent[];

			setBusinessEvents(items);
		}
		catch (error) {
			setBusinessEvents([]);
		}
		finally {
			setLoading(false);
		}
	}, [accountKey]);

	useEffect(() => {
		fetchBusinessEvents();
	}, [fetchBusinessEvents]);

	return {businessEvents, fetchBusinessEvents, loading};
}
