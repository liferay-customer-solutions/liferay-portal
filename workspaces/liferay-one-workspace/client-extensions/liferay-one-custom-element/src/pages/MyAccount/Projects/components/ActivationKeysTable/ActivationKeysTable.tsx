/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {useMemo, useState} from 'react';
import Button from '~/components/Button/Button';
import {
	ProjectActivationKey,
	useProjectActivationKeys,
} from '~/hooks/useProjectActivationKeys';
import {Word, translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import './ActivationKeysTable.css';

const PAGE_SIZE_OPTIONS = [5, 10, 20, 30, 50];

const STATUS_TABS: ('active' | 'all' | 'expired')[] = [
	'all',
	'active',
	'expired',
];

type ActivationKeysTableProps = {
	productName?: string;
};

function matchesSearch(row: ProjectActivationKey, search: string): boolean {
	return (
		row.name.toLowerCase().includes(search) ||
		row.domain.toLowerCase().includes(search)
	);
}

export default function ActivationKeysTable({
	productName,
}: ActivationKeysTableProps) {
	const {activationKeys, loading} = useProjectActivationKeys(productName);

	const [statusTab, setStatusTab] =
		useState<(typeof STATUS_TABS)[number]>('all');
	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(PAGE_SIZE_OPTIONS[0]);

	const statusCounts = useMemo(() => {
		const counts: Record<string, number> = {all: activationKeys.length};

		for (const row of activationKeys) {
			counts[row.status] = (counts[row.status] ?? 0) + 1;
		}

		return counts;
	}, [activationKeys]);

	const filteredRows = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return activationKeys.filter((row) => {
			if (statusTab !== 'all' && row.status !== statusTab) {
				return false;
			}

			if (search && !matchesSearch(row, search)) {
				return false;
			}

			return true;
		});
	}, [activationKeys, keywords, statusTab]);

	const paginatedRows = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredRows.slice(start, start + pageSize);
	}, [filteredRows, page, pageSize]);

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

				<div className="ml-auto">
					<ClayDropDown
						trigger={
							<Button
								appendIcon="caret-bottom"
								displayType="primary"
							>
								{translate('actions')}
							</Button>
						}
					>
						<ClayDropDown.ItemList>
							<ClayDropDown.Item>
								{translate('generate-new')}
							</ClayDropDown.Item>

							<ClayDropDown.Item>
								{translate('export-all-key-details-csv')}
							</ClayDropDown.Item>
						</ClayDropDown.ItemList>
					</ClayDropDown>
				</div>
			</div>

			{loading ? (
				<div className="p-4 text-neutral-7">{translate('loading')}</div>
			) : paginatedRows.length ? (
				<>
					<ClayTable borderless className="list-card-table">
						<ClayTable.Head>
							<ClayTable.Row>
								<ClayTable.Cell headingCell>
									<span className="d-flex flex-column">
										<span>
											{translate('environment-name')}
										</span>

										<span className="list-card-subtext">
											{translate('domain')}
										</span>
									</span>
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
										<span className="d-flex flex-column">
											<span className="fw-bold">
												{row.name}
											</span>

											<span className="list-card-subtext">
												{row.domain || '-'}
											</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="d-flex flex-column">
											<span>{`${row.startDate} -`}</span>

											<span>{row.expirationDate}</span>
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<span className="list-card-status">
											<span
												className="list-card-status-dot"
												style={{
													backgroundColor:
														getStatusColor(
															row.status
														),
												}}
											/>

											{translate(row.status as Word)}
										</span>
									</ClayTable.Cell>

									<ClayTable.Cell>
										<Button
											aria-label={translate('download')}
											borderless
											className="text-neutral-7"
											disabled={row.status === 'expired'}
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
