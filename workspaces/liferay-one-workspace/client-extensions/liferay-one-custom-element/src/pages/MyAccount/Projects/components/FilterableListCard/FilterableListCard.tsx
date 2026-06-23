/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {ReactNode, useMemo, useState} from 'react';
import Button from '~/components/Button/Button';
import {Word, translate} from '~/i18n';

import './FilterableListCard.css';

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50];

export type FilterOption = {label: string; value: string};

export type ListFilter<T> = {
	key: string;
	label: Word;
	matches: (item: T, values: string[]) => boolean;
	options: FilterOption[];
};

export type ListColumn<T> = {
	heading?: Word;
	key: string;
	render: (item: T) => ReactNode;
};

type FilterableListCardProps<T> = {
	action?: ReactNode;
	columns: ListColumn<T>[];
	defaultPageSize?: number;
	emptyLabel: Word;
	filters: ListFilter<T>[];
	items: T[];
	matchesSearch: (item: T, search: string) => boolean;
	onItemClick: (item: T) => void;
	rowKey: (item: T) => string;
	title?: Word;
};

type FilterSubPanelProps = {
	onApply: (values: string[]) => void;
	onBack: () => void;
	options: FilterOption[];
	selectedValues: string[];
	title: string;
};

function FilterSubPanel({
	onApply,
	onBack,
	options,
	selectedValues,
	title,
}: FilterSubPanelProps) {
	const [draftValues, setDraftValues] = useState<string[]>(selectedValues);

	const toggleValue = (value: string) =>
		setDraftValues((previous) =>
			previous.includes(value)
				? previous.filter((current) => current !== value)
				: [...previous, value]
		);

	return (
		<div className="list-card-filter-panel">
			<button
				className="list-card-filter-back"
				onClick={onBack}
				type="button"
			>
				<ClayIcon symbol="angle-left" />

				<span className="list-card-filter-title">{title}</span>
			</button>

			<div className="list-card-filter-options mt-3">
				{options.map(({label, value}) => (
					<ClayCheckbox
						checked={draftValues.includes(value)}
						key={value}
						label={label}
						onChange={() => toggleValue(value)}
					/>
				))}
			</div>

			<ClayButton
				className="list-card-filter-apply w-100"
				onClick={() => onApply(draftValues)}
			>
				{translate('add-filter')}
			</ClayButton>
		</div>
	);
}

export default function FilterableListCard<T>({
	action,
	columns,
	defaultPageSize = PAGE_SIZE_OPTIONS[0],
	emptyLabel,
	filters,
	items,
	matchesSearch,
	onItemClick,
	rowKey,
	title,
}: FilterableListCardProps<T>) {
	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(defaultPageSize);
	const [filterActive, setFilterActive] = useState(false);
	const [activeFilterKey, setActiveFilterKey] = useState<string | null>(null);
	const [selectedFilters, setSelectedFilters] = useState<
		Record<string, string[]>
	>({});

	const filteredItems = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return items.filter((item) => {
			if (search && !matchesSearch(item, search)) {
				return false;
			}

			return filters.every((filter) => {
				const values = selectedFilters[filter.key] ?? [];

				return !values.length || filter.matches(item, values);
			});
		});
	}, [filters, items, keywords, matchesSearch, selectedFilters]);

	const paginatedItems = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredItems.slice(start, start + pageSize);
	}, [filteredItems, page, pageSize]);

	const closeFilter = () => {
		setFilterActive(false);
		setActiveFilterKey(null);
	};

	const activeFilter = filters.find(
		(filter) => filter.key === activeFilterKey
	);

	return (
		<div className="list-card mt-3">
			{title && (
				<div className="list-card-header">{translate(title)}</div>
			)}

			<div className="align-items-center d-flex list-card-toolbar">
				<ClayDropDown
					active={filterActive}
					className="list-card-filter-dropdown"
					onActiveChange={(active) => {
						setFilterActive(active);

						if (!active) {
							setActiveFilterKey(null);
						}
					}}
					trigger={
						<Button
							appendIcon="caret-bottom"
							className="list-card-filter-button"
							displayType="secondary"
							prependIcon="filter"
						>
							{translate('filter')}
						</Button>
					}
				>
					{activeFilter ? (
						<FilterSubPanel
							onApply={(values) => {
								setSelectedFilters((previous) => ({
									...previous,
									[activeFilter.key]: values,
								}));
								setPage(1);
								closeFilter();
							}}
							onBack={() => setActiveFilterKey(null)}
							options={activeFilter.options}
							selectedValues={
								selectedFilters[activeFilter.key] ?? []
							}
							title={translate(activeFilter.label)}
						/>
					) : (
						<div className="list-card-filter-panel">
							<div className="list-card-filter-heading">
								{translate('filters')}
							</div>

							{filters.map((filter) => (
								<button
									className="align-items-center d-flex justify-content-between list-card-filter-category"
									key={filter.key}
									onClick={() =>
										setActiveFilterKey(filter.key)
									}
									type="button"
								>
									<span>{translate(filter.label)}</span>

									<ClayIcon symbol="angle-right" />
								</button>
							))}
						</div>
					)}
				</ClayDropDown>

				<ClayInput.Group className="list-card-search">
					<ClayInput.GroupItem>
						<ClayInput
							className="input-group-inset input-group-inset-after"
							onChange={(event) => {
								setPage(1);
								setKeywords(event.target.value);
							}}
							placeholder={translate('search')}
							type="text"
							value={keywords}
						/>

						<ClayInput.GroupInsetItem after tag="span">
							<ClayIcon
								className="text-neutral-7"
								symbol="search"
							/>
						</ClayInput.GroupInsetItem>
					</ClayInput.GroupItem>
				</ClayInput.Group>

				{action && (
					<div className="list-card-toolbar-action">{action}</div>
				)}
			</div>

			{paginatedItems.length ? (
				<>
					<ClayTable borderless className="list-card-table">
						<ClayTable.Head>
							<ClayTable.Row>
								{columns.map((column) => (
									<ClayTable.Cell
										headingCell
										key={column.key}
									>
										{column.heading
											? translate(column.heading)
											: null}
									</ClayTable.Cell>
								))}
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{paginatedItems.map((item) => (
								<ClayTable.Row
									key={rowKey(item)}
									onClick={() => onItemClick(item)}
								>
									{columns.map((column) => (
										<ClayTable.Cell key={column.key}>
											{column.render(item)}
										</ClayTable.Cell>
									))}
								</ClayTable.Row>
							))}
						</ClayTable.Body>
					</ClayTable>

					<div className="list-card-pagination">
						<ClayPaginationBarWithBasicItems
							activeDelta={pageSize}
							activePage={page}
							deltas={PAGE_SIZE_OPTIONS.map((label) => ({
								label,
							}))}
							labels={{
								paginationResults: translate(
									'showing-x-to-x-of-x'
								),
								perPageItems: translate('x-items'),
								selectPerPageItems: translate('x-items'),
							}}
							onDeltaChange={(delta) => {
								setPage(1);
								setPageSize(delta);
							}}
							onPageChange={setPage}
							totalItems={filteredItems.length}
						/>
					</div>
				</>
			) : (
				<div className="p-4 text-neutral-7">
					{translate(emptyLabel)}
				</div>
			)}
		</div>
	);
}
