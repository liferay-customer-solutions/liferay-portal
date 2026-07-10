/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayLoadingIndicator from '@clayui/loading-indicator';
import {useEffect} from 'react';
import {Outlet, useNavigate, useParams} from 'react-router-dom';
import RestrictedFeatureMessage from '~/components/RestrictedFeatureMessage/RestrictedFeatureMessage';
import {translate} from '~/i18n';
import {
	getCurrentUserId,
	getLastViewedProjectCookie,
	getSelectedAccountId,
	resolveProjectERC,
	setLastViewedProjectCookie,
	useUserProjects,
} from '~/pages/MyAccount/Projects/projects';

const BusinessEventsRedirect = () => {
	const navigate = useNavigate();

	const {projectERC} = useParams<{projectERC: string}>();

	const {hasAccountProjects, loading, projects} = useUserProjects();

	const userId = getCurrentUserId();
	const accountId = getSelectedAccountId();

	const isAccessible =
		Boolean(projectERC) &&
		projects.some(
			(project) => project.externalReferenceCode === projectERC
		);

	useEffect(() => {
		if (loading) {
			return;
		}

		if (isAccessible) {
			setLastViewedProjectCookie(accountId, projectERC as string, userId);

			return;
		}

		if (!projects.length) {
			return;
		}

		const resolvedProjectERC = resolveProjectERC(
			projects,
			getLastViewedProjectCookie(accountId, userId)
		);

		if (resolvedProjectERC) {
			navigate(`/${resolvedProjectERC}/business-events`, {replace: true});
		}
	}, [
		accountId,
		isAccessible,
		loading,
		navigate,
		projectERC,
		projects,
		userId,
	]);

	if (loading || (!isAccessible && !!projects.length)) {
		return (
			<div className="mx-auto">
				<ClayLoadingIndicator size="sm" />
			</div>
		);
	}

	if (!projects.length) {
		return (
			<RestrictedFeatureMessage
				message={
					hasAccountProjects
						? translate(
								'login-as-a-user-that-has-access-to-a-project-or-contact-your-project-administrator-to-add-you-to-a-project.'
							)
						: undefined
				}
			/>
		);
	}

	return <Outlet />;
};

export default BusinessEventsRedirect;
