/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import i18n from '~/i18n';

import {getResourceSummary} from '../utils';

import type {ConsoleUserProject} from '~/services/spring-boot/Console';

type SelectedProjectBannerProps = {
	project: ConsoleUserProject;
};

const SelectedProjectBanner = ({project}: SelectedProjectBannerProps) => (
	<div>
		<hr />

		<div className="align-items-center d-flex justify-content-between">
			<small className="font-weight-bold">
				{i18n.translate('project-selected')}
			</small>

			<span className="align-items-end d-flex flex-column">
				<small className="font-weight-bold m-0">
					{project.rootProjectId.toUpperCase()}
				</small>

				<small className="text-nowrap">
					{getResourceSummary(project)}
				</small>
			</span>
		</div>
	</div>
);

export default SelectedProjectBanner;
