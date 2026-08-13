/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useContext, useEffect} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import {
	ListViewContext,
	ListViewTypes,
} from '~/components/ListView/context/ListViewContextProvider';

import './ManagementToolbarResultsBar.css';

const ManagementToolbarResultsBar = () => {
	const location = useLocation();
	const navigate = useNavigate();
	const searchParams = new URLSearchParams(location.search);

	const filter = searchParams.get('filter');

	const [{filters}, dispatch] = useContext(ListViewContext);

	const handleRemoveItemFromFilter = (itemToRemove: string) => {
		if (!filter) {
			return;
		}

		const filterJSON = JSON.parse(decodeURIComponent(filter));

		delete filterJSON[itemToRemove];

		if (Object.keys(filterJSON).length) {
			searchParams.set('filter', JSON.stringify(filterJSON));
		}
		else {
			searchParams.delete('filter');
			searchParams.delete('filterSchema');
			searchParams.delete('page');
		}

		navigate({
			search: `?${searchParams.toString()}`,
		});
	};

	const onRemoveFilter = (filterName: string) => {
		dispatch({payload: filterName, type: ListViewTypes.SET_REMOVE_FILTER});

		handleRemoveItemFromFilter(filterName);
	};

	useEffect(() => {
		if (!filter) {
			filters.entries
				.filter(({value}) => value)
				.forEach((entry) =>
					dispatch({
						payload: entry.name,
						type: ListViewTypes.SET_REMOVE_FILTER,
					})
				);
		}
	}, [dispatch, filter, filters.entries]);

	const entries = filters.entries.filter(({value}) => value);

	if (!entries.length) {
		return null;
	}

	return (
		<div className="management-toolbar-active-filters">
			{entries.map((entry) => (
				<span
					className="management-toolbar-filter-tag"
					key={entry.name}
				>
					{`${entry.label}: ${
						Array.isArray(entry.value)
							? entry.value
									.map((entryValue) =>
										String(
											typeof entryValue === 'object'
												? entryValue?.label
												: entryValue
										)
									)
									.sort((entryA, entryB) =>
										entryA.localeCompare(entryB)
									)
									.join(', ')
							: entry.value
					}`}

					<button
						className="management-toolbar-filter-tag-close"
						onClick={() => onRemoveFilter(entry.name)}
						type="button"
					>
						<ClayIcon symbol="times" />
					</button>
				</span>
			))}
		</div>
	);
};

export default ManagementToolbarResultsBar;
