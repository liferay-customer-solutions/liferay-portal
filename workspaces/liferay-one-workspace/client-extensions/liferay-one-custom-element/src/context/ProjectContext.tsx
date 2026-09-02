/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {
	ReactNode,
	createContext,
	useContext,
	useEffect,
	useMemo,
	useState,
} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import {useUnassignedCommerce} from '~/hooks/useProjectCommerce';
import i18n from '~/i18n';
import {
	LAST_PROJECT_STORAGE_KEY,
	ONE_TIME_PURCHASES,
	UserProject,
	resolveDefaultProject,
	useUserProjects,
} from '~/pages/MyAccount/Projects/projects';

type ProjectContextValue = {
	loading: boolean;
	project?: UserProject;
	projectId: string;
	projects: UserProject[];
	selectedContractERC?: string;
	setSelectedContractERC: (contractERC: string) => void;
};

const ProjectContext = createContext<ProjectContextValue>(
	{} as ProjectContextValue
);

export function ProjectProvider({children}: {children: ReactNode}) {
	const {accountERC, projectERC} = useParams();
	const navigate = useNavigate();

	const {loading: projectsLoading, projects: userProjects} =
		useUserProjects();

	const {entitlements: unassignedEntitlements, loading: unassignedLoading} =
		useUnassignedCommerce();

	const projects = useMemo<UserProject[]>(() => {
		if (!unassignedEntitlements.length) {
			return userProjects;
		}

		return [
			...userProjects,
			{
				externalReferenceCode: ONE_TIME_PURCHASES,
				id: -1,
				name: i18n.translate('one-time-purchases'),
				unassigned: true,
			},
		];
	}, [unassignedEntitlements.length, userProjects]);

	const loading = projectsLoading || unassignedLoading;

	const projectId = projectERC ?? '';

	const project = useMemo(
		() => projects.find((item) => item.externalReferenceCode === projectId),
		[projectId, projects]
	);

	const [selectedContractERC, setSelectedContractERC] = useState<string>();

	useEffect(() => {
		setSelectedContractERC(undefined);
	}, [projectId]);

	const accessible = projects.some(
		(project) => project.externalReferenceCode === projectId
	);

	useEffect(() => {
		if (accessible) {
			localStorage.setItem(LAST_PROJECT_STORAGE_KEY, projectId);
		}
	}, [accessible, projectId]);

	useEffect(() => {
		if (loading || !projects.length || accessible) {
			return;
		}

		const target = resolveDefaultProject(projects);

		if (!target) {
			return;
		}

		navigate(
			`/${accountERC}/project/${target.externalReferenceCode}/products`,
			{replace: true}
		);
	}, [accessible, accountERC, loading, navigate, projects]);

	return (
		<ProjectContext.Provider
			value={{
				loading,
				project,
				projectId,
				projects,
				selectedContractERC,
				setSelectedContractERC,
			}}
		>
			{children}
		</ProjectContext.Provider>
	);
}

export function useProject() {
	return useContext(ProjectContext);
}
