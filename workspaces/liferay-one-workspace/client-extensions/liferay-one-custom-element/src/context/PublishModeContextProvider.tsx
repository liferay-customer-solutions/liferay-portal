/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode, createContext, useContext} from 'react';
import {PublishMode} from '~/pages/PublisherDashboard/pages/NewAppFlow/constants';

const PublishModeContext = createContext<PublishMode>(PublishMode.CREATE);

type PublishModeContextProviderProps = {
	children: ReactNode;
	mode: PublishMode;
};

export default function PublishModeContextProvider({
	children,
	mode,
}: PublishModeContextProviderProps) {
	return (
		<PublishModeContext.Provider value={mode}>
			{children}
		</PublishModeContext.Provider>
	);
}

export function usePublishMode() {
	return useContext(PublishModeContext);
}
