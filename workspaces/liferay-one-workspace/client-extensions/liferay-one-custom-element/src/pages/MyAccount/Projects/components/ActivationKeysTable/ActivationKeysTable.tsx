/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {useMemo, useState} from 'react';
import Button from '~/components/Button/Button';
import {Word, translate} from '~/i18n';
import {
	ACTIVATION_KEYS_MOCK,
	ActivationKeyRow,
	ActivationKeyStatus,
} from '~/pages/MyAccount/Projects/utils/activationMockDataConstants';

import './ActivationKeysTable.css';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 30, 50];

const STATUS_TABS: ('activated' | 'all' | 'expired' | 'not-activated')[] = [
	'all',
	'activated',
	'not-activated',
	'expired',
];

const STATUS_DOT_COLOR: Record<ActivationKeyStatus, string> = {
	'activated': 'var(--color-state-success)',
	'expired': 'var(--color-danger-l1)',
	'not-activated': 'var(--color-state-info)',
};

function matchesSearch(row: ActivationKeyRow, search: string): boolean {
	return (
		row.environmentName.toLowerCase().includes(search) ||
		row.description.toLowerCase().includes(search) ||
		row.hostName.toLowerCase().includes(search)
	);
}

function keyTypeSubtext(row: ActivationKeyRow): string {
	if (row.keyType === 'On-Premise') {
		return row.hostName || '-';
	}

	return translate('x-cluster-nodes-keys').replace(
		'{0}',
		String(row.clusterNodes ?? 0)
	);
}

export default function ActivationKeysTable() {
	const rows = ACTIVATION_KEYS_MOCK;

	const [statusTab, setStatusTab] = useState<(typeof STATUS_TABS)[number]>(
		'all'
	);
	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(PAGE_SIZE_OPTIONS[0]);
	const [checkedIds, setCheckedIds] = useState<string[]>([]);
	const [filterActive, setFilterActive] = useState(false);
	const [environmentTypes, setEnvironmentTypes] = useState<string[]>([]);

	const statusCounts = useMemo(() => {
		const counts: Record<string, number> = {all: rows.length};

		for (const row of rows) {
			counts[row.status] = (counts[row.status] ?? 0) + 1;
		}

		return counts;
	}, [rows]);

	const environmentTypeOptions = useMemo(
		() => Array.from(new Set(rows.map((row) => row.environmentType))).sort(),
		[rows]
	);

	const filteredRows = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return rows.filter((row) => {
			if (statusTab !== 'all' && row.status !== statusTab) {
				return false;
			}

			if (search && !matchesSearch(row, search)) {
				return false;
			}

			if (
				environmentTypes.length &&
				!environmentTypes.includes(row.environmentType)
			) {
				return false;
			}

			return true;
		});
	}, [environmentTypes, keywords, rows, statusTab]);

	const paginatedRows = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredRows.slice(start, start + pageSize);
	}, [filteredRows, page, pageSize]);

	const allChecked =
		!!paginatedRows.length &&
		paginatedRows.every((row) => checkedIds.includes(row.id));

	const toggleAll = () =>
		setCheckedIds(allChecked ? [] : paginatedRows.map((row) => row.id));

	const toggleRow = (id: string) =>
		setCheckedIds((previous) =>
			previous.includes(id)
				? previous.filter((current) => current !== id)
				: [...previous, id]
		);

	const toggleEnvironmentType = (value: string) =>
		setEnvironmentTypes((previous) =>
			previous.includes(value)
				? previous.filter((current) => current !== value)
				: [...previous, value]
		);

	return (
		<div className="activation-keys-table mt-3">
			<div className="align-items-center d-flex justify-content-between mb-3">
				<h2 className="m-0">{translate('activation-keys')}</h2>

				<div className="activation-keys-table-tabs">
					{STATUS_TABS.map((tab) => (
						<button
							className={
								statusTab === tab
									? 'activation-keys-table-tab active'
									: 'activation-keys-table-tab'
							}
							key={tab}
							onClick={() => {
								setStatusTab(tab);
								setPage(1);
							}}
							type="button"
						>
							{`${translate(tab as Word)} (${
								statusCounts[tab] ?? 0
							})`}
						</button>
					))}
				</div>
			</div>

			<div className="activation-keys-table-toolbar">
				<ClayInput.Group className="activation-keys-table-search">
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

				<ClayDropDown
					active={filterActive}
					onActiveChange={setFilterActive}
					trigger={
						<Button
							displayType="secondary"
							prependIcon="filter"
						>
							{translate('filter')}
						</Button>
					}
				>
					<div className="p-3">
						<div className="list-card-filter-heading">
							{translate('environment-type')}
						</div>

						{environmentTypeOptions.map((option) => (
							<ClayCheckbox
								checked={environmentTypes.includes(option)}
								key={option}
								label={option}
								onChange={() => {
									setPage(1);
									toggleEnvironmentType(option);
								}}
							/>
						))}
					</div>
				</ClayDropDown>

				<div className="ml-auto">
					<ClayDropDown
						trigger={
							<Button appendIcon="caret-bottom" displayType="primary">
								{translate('actions')}
							</Button>
						}
					>
						<ClayDropDown.ItemList>
							<ClayDropDown.Item>
								{translate('generate-new')}
							</ClayDropDown.Item>

							<ClayDropDown.Item>
								{translate('renew')}
							</ClayDropDown.Item>

							<ClayDropDown.Item>
								{translate('deactivate')}
							</ClayDropDown.Item>

							<ClayDropDown.Item>
								{translate('export-all-key-details-csv')}
							</ClayDropDown.Item>
						</ClayDropDown.ItemList>
					</ClayDropDown>
				</div>
			</div>

			{paginatedRows.length ? (
				<>
					<ClayTable borderless className="list-card-table">
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									<ClayCheckbox
										checked={allChecked}
										onChange={toggleAll}
									/>
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									<span className="d-flex flex-column">
										<span>
											{translate('environment-name')}
										</span>

										<span className="list-card-subtext">
											{translate('description')}
										</span>
									</span>
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									<span className="d-flex flex-column">
										<span>{translate('key-type')}</span>

										<span className="list-card-subtext">
											{translate(
												'host-name-cluster-size'
											)}
										</span>
									</span>
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('environment-type')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									<span className="d-flex flex-column">
										<span>{translate('start-date')}</span>

										<span className="list-card-subtext">
											{translate('expiration-date')}
										</span>
									</span>
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									{translate('status')}
								</ClayTable.Cell>

								<ClayTable.Cell headingCell>
									<ClayIcon symbol="download" />
								</ClayTable.Cell>
							</ClayTable.Row>
						</ClayTable.Head>

						<ClayTable.Body>
							{paginatedRows.map((row) => (
								<ClayTable.Row key={row.id}>
									<ClayTable.Cell>
										<ClayCheckbox
											checked={checkedIds.includes(
												row.id
											)}
											onChange={() => toggleRow(row.id)}
										/>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="d-flex flex-column">
											<span className='fw-bold'>
												{row.environmentName}
											</span>

											<span className="list-card-subtext">
												{row.description}
											</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="d-flex flex-column">
											<span className='fw-bold'>
												{row.keyType}
											</span>

											<span className="list-card-subtext">
												{keyTypeSubtext(row)}
											</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="d-flex flex-column">
											<span className='fw-bold'>
												{row.environmentType}
											</span>

											<span className="list-card-subtext">
												{translate(
													row.complimentary
														? 'complimentary'
														: 'subscription'
												)}
											</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="d-flex flex-column">
											<span>{`${row.startDate}`}</span>

											<span>{row.expirationDate}</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="list-card-status">
											<span
												className="list-card-status-dot"
												style={{
													backgroundColor:
														STATUS_DOT_COLOR[
															row.status
														],
												}}
											/>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<Button
											aria-label={translate('download')}
											borderless
											className="text-neutral-7"
											displayType="unstyled"
											prependIcon="download"
										/>
									</ClayTable.Cell>
								</ClayTable.Row>
							))}
						</ClayTable.Body>
					</ClayTable>

					<div className="list-card-pagination">
						<ClayPaginationBarWithBasicItems
							activeDelta={pageSize}
							activePage={page}
							deltas={PAGE_SIZE_OPTIONS.map((label) => ({label}))}
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
							totalItems={filteredRows.length}
						/>
					</div>
				</>
			) : (
				<div className="p-4 text-neutral-7">
					{translate('no-activation-keys-yet')}
				</div>
			)}
		</div>
	);
}
