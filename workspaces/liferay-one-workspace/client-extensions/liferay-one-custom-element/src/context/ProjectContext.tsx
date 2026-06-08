/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	ReactNode,
	createContext,
	useCallback,
	useContext,
	useState,
} from 'react';

import {
	LAST_PROJECT_STORAGE_KEY,
	resolveProjectId,
} from '../pages/MyAccount/Projects/projects';

type ProjectContextValue = {
	projectId: string;
	setProjectId: (projectId: string) => void;
};

const ProjectContext = createContext<ProjectContextValue>(
	{} as ProjectContextValue
);

export function ProjectProvider({children}: {children: ReactNode}) {
	const [projectId, setProjectIdState] = useState(() => resolveProjectId());

	const setProjectId = useCallback((nextProjectId: string) => {
		localStorage.setItem(LAST_PROJECT_STORAGE_KEY, nextProjectId);

		setProjectIdState(nextProjectId);
	}, []);

	return (
		<ProjectContext.Provider value={{projectId, setProjectId}}>
			{children}
		</ProjectContext.Provider>
	);
}

export function useProject() {
	return useContext(ProjectContext);
}
