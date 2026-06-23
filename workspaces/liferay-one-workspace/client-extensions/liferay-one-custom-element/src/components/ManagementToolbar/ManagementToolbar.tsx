/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayManagementToolbar from '@clayui/management-toolbar';
import {ReactElement, useContext} from 'react';
import {ListViewContext} from '~/components/ListView/context/ListViewContextProvider';
import ManagementToolbarFilter from '~/components/ManagementToolbarFilters/ManagementToolbarFilters';
import ManagementToolbarResultsBar from '~/components/ManagementToolbarResultsBar/ManagementToolbarResultsBar';
import ManagementToolbarSearch from '~/components/ManagementToolbarSearch/ManagementToolbarSearch';
import {
	FilterSchemaOption,
	filterSchema as filterSchemas,
} from '~/types/filters';

export type ManagementToolbarProps = {
	actionButton?: (
		filter: Record<string, unknown>,
		filterSchema?: FilterSchemaOption
	) => ReactElement;

	filterSchema?: FilterSchemaOption;
	searchVisible?: boolean;
	totalItems: number;
};

const ManagementToolbar: React.FC<ManagementToolbarProps> = ({
	actionButton,
	filterSchema,
	searchVisible = false,
	totalItems,
}) => {
	const [{filters}] = useContext(ListViewContext);

	return (
		<>
			<ClayManagementToolbar>
				<div className="d-flex justify-content-between w-100">
					{filterSchema && (
						<ManagementToolbarFilter
							filterSchema={
								filterSchemas[
									filterSchema as FilterSchemaOption
								]
							}
						/>
					)}

					{!!searchVisible && (
						<div className="d-flex w-100">
							<ManagementToolbarSearch />
							{actionButton &&
								actionButton(filters.filter, filterSchema)}
						</div>
					)}
				</div>

				{!!filters.entries?.filter(({value}) => value).length && (
					<ManagementToolbarResultsBar totalItems={totalItems} />
				)}
			</ClayManagementToolbar>
		</>
	);
};

export default ManagementToolbar;
