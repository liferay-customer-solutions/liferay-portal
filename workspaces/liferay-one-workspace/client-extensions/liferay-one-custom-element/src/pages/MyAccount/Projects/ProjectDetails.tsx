/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect} from 'react';
import {useParams} from 'react-router-dom';

import {useProperties} from '../../../context/PropertiesContext';

export default function ProjectDetails() {
	const {accountId} = useProperties();
	const {id} = useParams();

	useEffect(() => {
		if (id) {
			localStorage.setItem('liferay-one:last-project', id);
		}
	}, [id]);

	return <div>{accountId}</div>;
}
