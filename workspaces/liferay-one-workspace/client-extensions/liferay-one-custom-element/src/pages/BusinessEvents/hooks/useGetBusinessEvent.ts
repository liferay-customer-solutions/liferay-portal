/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useCallback, useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {translate} from '~/i18n';
import {IBusinessEvent} from '~/pages/BusinessEvents/types';
import {Liferay} from '~/services/liferay/liferay';
import {getBusinessEventById} from '~/services/spring-boot/Jira';

export default function useGetBusinessEvent(
	id: string,
	projectERC: string
): {
	businessEvent: IBusinessEvent | undefined;
	fetchBusinessEvent: () => Promise<void>;
	loading: boolean;
} {
	const [businessEvent, setBusinessEvent] = useState<
		IBusinessEvent | undefined
	>(undefined);

	const [loading, setLoading] = useState(true);

	const navigate = useNavigate();

	const fetchBusinessEvent = useCallback(async () => {
		if (!projectERC) {
			return;
		}

		setLoading(true);

		try {
			const businessEventResponse = await getBusinessEventById(
				id,
				projectERC
			);

			setBusinessEvent(businessEventResponse as IBusinessEvent);
		}
		catch (error) {
			Liferay.Util.openToast({
				message: translate('an-unexpected-error-occurred'),
				type: 'danger',
			});

			navigate(`/${projectERC}/business-events`);
		}
		finally {
			setLoading(false);
		}
	}, [id, navigate, projectERC]);

	useEffect(() => {
		if (!id) {
			setLoading(true);

			return;
		}

		fetchBusinessEvent();
	}, [fetchBusinessEvent, id]);

	return {businessEvent, fetchBusinessEvent, loading};
}
