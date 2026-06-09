/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import {Link} from 'react-router-dom';

import {translate} from '../../../i18n';

import './TableHeader.css';
import Filter from '../../Filter/Filter';
import {IFilterOption} from '../../Filter/Filter';
import FilterResults from '../../Filter/FilterResults/FilterResults';
import SearchBar from '../../SearchBar/SearchBar';

interface IProps {
	availableFilters: IFilterOption[];
	hasCreatePermissions: boolean;
	onFilterChange: (selectedFilters: IFilterOption[]) => void;
	onSearchChange: (searchTerm: string) => void;
	searchResultsCount: number;
	searchTerm: string;
	selectedFilters: IFilterOption[];
}

const TableHeader = ({
	availableFilters,
	hasCreatePermissions,
	onFilterChange,
	onSearchChange,
	searchResultsCount,
	searchTerm,
	selectedFilters,
}: IProps) => {
	return (
		<div className="d-flex flex-column mt-4">
			<div className="be-table-header p-3">
				<div className="d-flex justify-content-between">
					<div className="d-flex">
						<SearchBar
							isBusinessEvent={true}
							onSearchSubmit={(term: string) => {
								onSearchChange(term);
							}}
						/>

						<Filter
							availableFilters={availableFilters}
							onChange={onFilterChange}
							selectedFilters={selectedFilters}
						/>
					</div>

					{hasCreatePermissions && (
						<Link to="add">
							<Button className="be-create-event">
								{translate('create-event')}
							</Button>
						</Link>
					)}
				</div>

				<FilterResults
					onChange={onFilterChange}
					searchResultsCount={searchResultsCount}
					searchTerm={searchTerm}
					selectedFilters={selectedFilters}
				/>
			</div>
		</div>
	);
};

export default TableHeader;
