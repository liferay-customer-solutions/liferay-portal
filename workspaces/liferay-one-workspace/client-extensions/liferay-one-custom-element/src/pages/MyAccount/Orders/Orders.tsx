/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown from '@clayui/drop-down';
import {ClayCheckbox, ClayInput, ClayToggle} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import {ClayPaginationBarWithBasicItems} from '@clayui/pagination-bar';
import ClayTable from '@clayui/table';
import {format} from 'date-fns';
import {useMemo, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import Button from '~/components/Button/Button';
import EmptyState from '~/components/EmptyState/EmptyState';
import Page from '~/components/Page/Page';
import RowActionsMenu from '~/components/RowActionsMenu/RowActionsMenu';
import {useOneContext} from '~/context/OneContextProvider';
import {useFetch} from '~/hooks/useFetch';
import i18n, {Word, translate} from '~/i18n';
import {canAccessOrders} from '~/pages/MyAccount/AccountMembers/accountRoles';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';
import {Liferay} from '~/services/liferay/liferay';
import {
	OrderCustomFields,
	PaymentStatus,
	getOrderStatusLabel,
} from '~/utils/orderUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import './Orders.css';

import type {APIResponse} from '~/types/api';
import type {PlacedOrder} from '~/types/orders';

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50];

const FILTER_SEARCH_MIN_OPTIONS = 10;

const INVOICE_STATUS_OPTIONS: {label: Word; value: number}[] = [
	{label: 'paid', value: PaymentStatus.PAID},
	{label: 'not-required', value: PaymentStatus.NOT_REQUIRED},
	{label: 'unpaid', value: PaymentStatus.PENDING},
	{label: 'pending', value: PaymentStatus.PAYMENT_PENDING},
	{label: 'failed', value: PaymentStatus.FAILED},
	{label: 'canceled', value: PaymentStatus.CANCELED},
];

type FilterCategory = 'invoice-status' | 'project';

type FilterOption = {label: string; value: number | string};

export function getProjectName(order: PlacedOrder): string {
	const customFields = order.customFields ?? {};

	const projectName = customFields[OrderCustomFields.PROJECT_NAME];

	if (projectName) {
		return projectName;
	}

	const projects = safeJSONParse<{name: string}[]>(
		customFields[OrderCustomFields.KORONEIKI_PROJECT],
		[]
	);

	return projects[0]?.name ?? '';
}

export function getOrderTotal(order: PlacedOrder): string {
	const {summary, totalFormatted} = order as PlacedOrder & {
		summary?: {totalFormatted?: string};
		totalFormatted?: string;
	};

	return summary?.totalFormatted ?? totalFormatted ?? '$0.00';
}

type FilterSubPanelProps = {
	hasExclude?: boolean;
	onApply: (values: (number | string)[], exclude: boolean) => void;
	onBack: () => void;
	options: FilterOption[];
	selectedExclude?: boolean;
	selectedValues: (number | string)[];
	title: string;
};

function FilterSubPanel({
	hasExclude = false,
	onApply,
	onBack,
	options,
	selectedExclude = false,
	selectedValues,
	title,
}: FilterSubPanelProps) {
	const [keywords, setKeywords] = useState('');
	const [draftValues, setDraftValues] =
		useState<(number | string)[]>(selectedValues);
	const [draftExclude, setDraftExclude] = useState(selectedExclude);

	const filteredOptions = options.filter(({label}) =>
		label.toLowerCase().includes(keywords.trim().toLowerCase())
	);

	const toggleValue = (value: number | string) =>
		setDraftValues((previous) =>
			previous.includes(value)
				? previous.filter((current) => current !== value)
				: [...previous, value]
		);

	return (
		<div className="orders-filter-panel">
			<button
				className="orders-filter-back"
				onClick={onBack}
				type="button"
			>
				<ClayIcon symbol="angle-left" />

				<span className="orders-filter-title">{title}</span>
			</button>

			{options.length > FILTER_SEARCH_MIN_OPTIONS && (
				<ClayInput
					className="mt-3 orders-filter-search"
					onChange={(event) => setKeywords(event.target.value)}
					placeholder={translate('search')}
					type="text"
					value={keywords}
				/>
			)}

			{hasExclude && (
				<div className="align-items-center d-flex mt-3 orders-filter-exclude">
					<span className="mr-2">{translate('exclude')}</span>

					<ClayToggle
						className="orders-filter-exclude-toggle"
						onToggle={setDraftExclude}
						toggled={draftExclude}
					/>
				</div>
			)}

			<div className="mt-3 orders-filter-options">
				{filteredOptions.map(({label, value}) => (
					<ClayCheckbox
						checked={draftValues.includes(value)}
						key={value}
						label={label}
						onChange={() => toggleValue(value)}
					/>
				))}
			</div>

			<ClayButton
				className="mt-3 orders-filter-apply w-100"
				onClick={() => onApply(draftValues, draftExclude)}
			>
				{translate('add-filter')}
			</ClayButton>
		</div>
	);
}

export default function Orders() {
	const accountId = Liferay.CommerceContext?.account?.accountId;
	const channelId = Liferay.CommerceContext?.commerceChannelId;

	const navigate = useNavigate();

	const {myUserAccount, userAccountModel} = useOneContext();

	const [keywords, setKeywords] = useState('');
	const [page, setPage] = useState(1);
	const [pageSize, setPageSize] = useState(PAGE_SIZE_OPTIONS[0]);
	const [filterActive, setFilterActive] = useState(false);
	const [filterCategory, setFilterCategory] = useState<FilterCategory | null>(
		null
	);
	const [projectFilter, setProjectFilter] = useState<{
		exclude: boolean;
		values: string[];
	}>({exclude: false, values: []});
	const [invoiceStatusFilter, setInvoiceStatusFilter] = useState<number[]>(
		[]
	);

	const {
		data,
		error,
		isLoading: loading,
	} = useFetch<APIResponse<PlacedOrder>>(
		accountId && channelId
			? `/o/headless-commerce-delivery-order/v1.0/channels/${channelId}/accounts/${accountId}/placed-orders`
			: null,
		{
			params: {
				nestedFields: 'placedOrderItems',
				pageSize: 100,
				sort: 'createDate:desc',
			},
		}
	);

	const orders = useMemo(() => data?.items ?? [], [data]);

	const projectOptions = useMemo<FilterOption[]>(() => {
		const names = new Set<string>();

		orders.forEach((order) => {
			const name = getProjectName(order);

			if (name) {
				names.add(name);
			}
		});

		return Array.from(names)
			.sort()
			.map((name) => ({label: name, value: name}));
	}, [orders]);

	const filteredOrders = useMemo(() => {
		const search = keywords.trim().toLowerCase();

		return orders.filter((order) => {
			const projectName = getProjectName(order);

			if (
				search &&
				!String(order.id).toLowerCase().includes(search) &&
				!projectName.toLowerCase().includes(search)
			) {
				return false;
			}

			if (projectFilter.values.length) {
				const matches = projectFilter.values.includes(projectName);

				if (projectFilter.exclude ? matches : !matches) {
					return false;
				}
			}

			if (
				invoiceStatusFilter.length &&
				!invoiceStatusFilter.includes(order.paymentStatus)
			) {
				return false;
			}

			return true;
		});
	}, [invoiceStatusFilter, keywords, orders, projectFilter]);

	const paginatedOrders = useMemo(() => {
		const start = (page - 1) * pageSize;

		return filteredOrders.slice(start, start + pageSize);
	}, [filteredOrders, page, pageSize]);

	const activeFilters = useMemo(() => {
		const filters: {
			label: string;
			onRemove: () => void;
			value: string;
		}[] = [];

		projectFilter.values.forEach((name) => {
			filters.push({
				label: projectFilter.exclude
					? `${translate('project')}: ${translate('exclude')} ${name}`
					: `${translate('project')}: ${name}`,
				onRemove: () => {
					setProjectFilter((previous) => ({
						...previous,
						values: previous.values.filter(
							(current) => current !== name
						),
					}));
					setPage(1);
				},
				value: `project-${name}`,
			});
		});

		invoiceStatusFilter.forEach((status) => {
			const option = INVOICE_STATUS_OPTIONS.find(
				(current) => current.value === status
			);

			filters.push({
				label: `${translate('invoice-status')}: ${
					option ? translate(option.label) : String(status)
				}`,
				onRemove: () => {
					setInvoiceStatusFilter((previous) =>
						previous.filter((current) => current !== status)
					);
					setPage(1);
				},
				value: `invoice-status-${status}`,
			});
		});

		return filters;
	}, [invoiceStatusFilter, projectFilter]);

	const closeFilter = () => {
		setFilterActive(false);
		setFilterCategory(null);
	};

	if (myUserAccount && !canAccessOrders(userAccountModel)) {
		return (
			<Page>
				<EmptyState
					className="mt-5"
					title={translate('you-do-not-have-access-to-this-page')}
					type="NO_ACCESS"
				/>
			</Page>
		);
	}

	return (
		<Page
			description={i18n.translate(
				'manage-all-your-orders-across-different-platform'
			)}
			pageRendererProps={{error, isLoading: loading}}
			title={i18n.translate('orders-list')}
		>
			<div className="mt-3 orders-card">
				<div className="align-items-center d-flex orders-toolbar">
					<ClayDropDown
						active={filterActive}
						className="orders-filter-dropdown"
						onActiveChange={(active) => {
							setFilterActive(active);

							if (!active) {
								setFilterCategory(null);
							}
						}}
						trigger={
							<Button
								appendIcon="caret-bottom"
								className="orders-filter-button"
								displayType="secondary"
								prependIcon="filter"
							>
								{translate('filter')}
							</Button>
						}
					>
						{filterCategory === null ? (
							<div className="orders-filter-panel">
								<div className="orders-filter-heading">
									{translate('filters')}
								</div>

								{(
									[
										{key: 'project', label: 'project'},
										{
											key: 'invoice-status',
											label: 'invoice-status',
										},
									] as {key: FilterCategory; label: Word}[]
								).map(({key, label}) => (
									<button
										className="align-items-center d-flex justify-content-between orders-filter-category"
										key={key}
										onClick={() => setFilterCategory(key)}
										type="button"
									>
										<span>{translate(label)}</span>

										<ClayIcon symbol="angle-right" />
									</button>
								))}
							</div>
						) : filterCategory === 'project' ? (
							<FilterSubPanel
								hasExclude
								onApply={(values, exclude) => {
									setProjectFilter({
										exclude,
										values: values as string[],
									});
									setPage(1);
									closeFilter();
								}}
								onBack={() => setFilterCategory(null)}
								options={projectOptions}
								selectedExclude={projectFilter.exclude}
								selectedValues={projectFilter.values}
								title={translate('project')}
							/>
						) : (
							<FilterSubPanel
								onApply={(values) => {
									setInvoiceStatusFilter(values as number[]);
									setPage(1);
									closeFilter();
								}}
								onBack={() => setFilterCategory(null)}
								options={INVOICE_STATUS_OPTIONS.map(
									({label, value}) => ({
										label: translate(label),
										value,
									})
								)}
								selectedValues={invoiceStatusFilter}
								title={translate('invoice-status')}
							/>
						)}
					</ClayDropDown>

					<ClayInput.Group className="orders-search">
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
				</div>

				{!!activeFilters.length && (
					<div className="orders-active-filters">
						{activeFilters.map(({label, onRemove, value}) => (
							<span className="orders-filter-tag" key={value}>
								{label}

								<button
									className="orders-filter-tag-close"
									onClick={onRemove}
									type="button"
								>
									<ClayIcon symbol="times" />
								</button>
							</span>
						))}
					</div>
				)}

				{paginatedOrders.length ? (
					<>
						<ClayTable borderless className="orders-table">
							<ClayTable.Head>
								<ClayTable.Row>
									<ClayTable.Cell headingCell>
										{translate('id')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('date')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('total')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('project')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell>
										{translate('status')}
									</ClayTable.Cell>

									<ClayTable.Cell headingCell />
								</ClayTable.Row>
							</ClayTable.Head>

							<ClayTable.Body>
								{paginatedOrders.map((order) => {
									return (
										<ClayTable.Row key={order.id}>
											<ClayTable.Cell>
												<span className="orders-id">
													{order.id}
												</span>
											</ClayTable.Cell>

											<ClayTable.Cell>
												{order.createDate
													? format(
															new Date(
																order.createDate
															),
															'MMM d, yyyy'
														)
													: '-'}
											</ClayTable.Cell>

											<ClayTable.Cell>
												{getOrderTotal(order)}
											</ClayTable.Cell>

											<ClayTable.Cell>
												{getProjectName(order) || '-'}
											</ClayTable.Cell>

											<ClayTable.Cell>
												<span className="align-items-center d-flex">
													<span
														className="orders-status-dot"
														style={{
															backgroundColor:
																getStatusColor(
																	getOrderStatusLabel(
																		order
																	).toLowerCase()
																),
														}}
													/>

													{getOrderStatusLabel(order)}
												</span>
											</ClayTable.Cell>

											<ClayTable.Cell>
												<RowActionsMenu
													actions={[
														{
															label: 'view-details',
															onClick: () =>
																navigate(
																	String(
																		order.id
																	)
																),
														},
													]}
												/>
											</ClayTable.Cell>
										</ClayTable.Row>
									);
								})}
							</ClayTable.Body>
						</ClayTable>

						<div className="orders-pagination">
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
								totalItems={filteredOrders.length}
							/>
						</div>
					</>
				) : (
					<div className="p-4 text-neutral-7">
						{translate('no-orders-yet')}
					</div>
				)}
			</div>
		</Page>
	);
}
