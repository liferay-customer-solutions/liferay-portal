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

import Button from '../../../components/Button/Button';
import Page from '../../../components/Page';
import {
	OrderCustomFields,
	PaymentStatus,
	getOrderStatusLabel,
	orderWorkflowDisplayType,
} from '../../../enums/Order';
import {useFetch} from '../../../hooks/useFetch';
import i18n, {Word, translate} from '../../../i18n';
import {Liferay} from '../../../liferay/liferay';
import {safeJSONParse} from '../../../utils/util';

import './Orders.css';

const PAGE_SIZE_OPTIONS = [10, 20, 30, 50];

const STATUS_DOT_COLORS: {[key: string]: string} = {
	info: '#2e5aac',
	secondary: '#6b6c7e',
	success: '#287d3c',
	warning: '#b95000',
};

const INVOICE_STATUS_OPTIONS: {label: Word; value: number}[] = [
	{label: 'paid', value: PaymentStatus.PAID},
	{label: 'unpaid', value: PaymentStatus.PENDING},
	{label: 'pending', value: PaymentStatus.PAYMENT_PENDING},
	{label: 'failed', value: PaymentStatus.FAILED},
	{label: 'canceled', value: PaymentStatus.CANCELED},
];

type FilterCategory = 'invoice-status' | 'project';

type FilterOption = {label: string; value: number | string};

function getProjectName(order: PlacedOrder): string {
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

function getOrderTotal(order: PlacedOrder): string {
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

	const selectedOptions = options.filter(({value}) =>
		draftValues.includes(value)
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

			<ClayInput
				className="mt-3 orders-filter-search"
				onChange={(event) => setKeywords(event.target.value)}
				placeholder={translate('search')}
				type="text"
				value={keywords}
			/>

			{!!selectedOptions.length && (
				<div className="mt-2 orders-filter-tags">
					{selectedOptions.map(({label, value}) => (
						<span className="orders-filter-tag" key={value}>
							{label}

							<button
								className="orders-filter-tag-close"
								onClick={() => toggleValue(value)}
								type="button"
							>
								<ClayIcon symbol="times" />
							</button>
						</span>
					))}
				</div>
			)}

			{hasExclude && (
				<div className="align-items-center d-flex justify-content-between mt-3 orders-filter-exclude">
					<span>{translate('exclude')}</span>

					<ClayToggle
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

	const {data, error, loading} = useFetch<APIResponse<PlacedOrder>>(
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

	const closeFilter = () => {
		setFilterActive(false);
		setFilterCategory(null);
	};

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
									const displayType =
										orderWorkflowDisplayType[
											order.orderStatusInfo
												?.code as keyof typeof orderWorkflowDisplayType
										];

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
																STATUS_DOT_COLORS[
																	displayType
																] ??
																STATUS_DOT_COLORS.secondary,
														}}
													/>

													{getOrderStatusLabel(order)}
												</span>
											</ClayTable.Cell>

											<ClayTable.Cell>
												<ClayButton
													aria-label={translate(
														'order-details'
													)}
													borderless
													className="text-neutral-7"
													displayType="unstyled"
												>
													<ClayIcon symbol="ellipsis-v" />
												</ClayButton>
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
