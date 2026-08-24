/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useSWR from 'swr';
import {Liferay} from '~/services/liferay/liferay';
import Console from '~/services/spring-boot/Console';

const useGetResourceInfo = () => {
	const {data: projectsUsage, isLoading} = useSWR(
		`/projects-usage/${Liferay.ThemeDisplay.getUserEmailAddress()}`,
		() => Console.getProjectsUsage()
	);

	return {
		isLoading,
		projectsUsage,
	};
};

export default useGetResourceInfo;
