/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Liferay} from '~/services/liferay';

import ProjectCardTeaser from './components/ProjectCardTeaser';
import TilesCard from './components/TilesCard';
import useAtLeastOneProject from './hooks/useAuthenticatedUser';

import './Home.css';

const Home = () => {
	const {atLeastOneProject} = useAtLeastOneProject();
	const isLogged = Liferay.ThemeDisplay.isSignedIn();

	return (
		<div className="home-container mt-4">
			<div className="">
				<ProjectCardTeaser />
			</div>

			<div>
				<TilesCard
					atLeastOneProject={atLeastOneProject}
					isLogged={isLogged}
				/>
			</div>
		</div>
	);
};

export default Home;
