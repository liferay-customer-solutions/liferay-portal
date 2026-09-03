/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import {ClayTooltipProvider} from '@clayui/tooltip';
import {format} from 'date-fns';
import {MouseEvent} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import Button from '~/components/Button/Button';
import Page from '~/components/Page/Page';
import {
	ProjectActivationKey,
	useProjectActivationKeys,
} from '~/hooks/useProjectActivationKeys';
import {useProjectProducts} from '~/hooks/useProjectCommerce';
import i18n, {translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import FilterableListCard, {
	ListColumn,
	ListFilter,
} from '../components/FilterableListCard/FilterableListCard';
import useLicenseKeyActions from './hooks/useLicenseKeyActions';

function matchesSearch(row: ProjectActivationKey, search: string): boolean {
	return (
		row.name.toLowerCase().includes(search) ||
		row.domain.toLowerCase().includes(search) ||
		row.description.toLowerCase().includes(search)
	);
}

function formatDateBound(value: string): string {
	const bound = value.split(':')[0];
	const date = value.slice(bound.length + 1);

	return `${translate(bound === 'after' ? 'after' : 'before')} ${format(
		new Date(date),
		'MMM d, yyyy'
	)}`;
}

function matchesDateBound(dateValue: string, values: string[]): boolean {
	return values.every((value) => {
		const bound = value.split(':')[0];
		const date = value.slice(bound.length + 1);

		if (!dateValue) {
			return false;
		}

		return bound === 'after' ? dateValue >= date : dateValue <= date;
	});
}

function stopAnd(callback: () => void) {
	return (event: MouseEvent) => {
		event.stopPropagation();

		callback();
	};
}

type KebabActionsProps = {
	onDeactivate: () => void;
	onDownload: () => void;
	onReactivate: () => void;
	onRenew: () => void;
	onView: () => void;
	row: ProjectActivationKey;
};

function KebabActions({
	onDeactivate,
	onDownload,
	onReactivate,
	onRenew,
	onView,
	row,
}: KebabActionsProps) {
	return (
		<ClayDropDown
			trigger={
				<Button
					borderless
					className="text-neutral-7"
					displayType="unstyled"
					onClick={(event) => event.stopPropagation()}
					prependIcon="ellipsis-v"
				/>
			}
		>
			<ClayDropDown.ItemList>
				<ClayDropDown.Item onClick={stopAnd(onView)}>
					{translate('view')}
				</ClayDropDown.Item>

				<ClayDropDown.Item
					disabled={!row.active}
					onClick={stopAnd(onDownload)}
				>
					{translate('download')}
				</ClayDropDown.Item>

				<ClayDropDown.Item onClick={stopAnd(onRenew)}>
					{translate('renew')}
				</ClayDropDown.Item>

				{row.active ? (
					<ClayDropDown.Item
						className="text-danger"
						onClick={stopAnd(onDeactivate)}
					>
						{translate('deactivate')}
					</ClayDropDown.Item>
				) : (
					<ClayDropDown.Item onClick={stopAnd(onReactivate)}>
						{translate('reactivate')}
					</ClayDropDown.Item>
				)}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}

export default function LicenseKeys() {
	const {projectId = ''} = useParams();
	const navigate = useNavigate();

	const {activationKeys, loading, revalidate} = useProjectActivationKeys();
	const {products} = useProjectProducts(projectId);

	const {
		handleDeactivate,
		handleDownload,
		handleNewKey,
		handleReactivate,
		handleRenew,
	} = useLicenseKeyActions({
		products,
		projectExternalReferenceCode: projectId,
		revalidate,
	});

	const columns: ListColumn<ProjectActivationKey>[] = [
		{
			heading: 'environment-name',
			key: 'environment-name',
			render: (row) => (
				<span className="d-flex flex-column">
					<span className="fw-bold">{row.name}</span>

					<span className="list-card-subtext">
						{row.domain || '-'}
					</span>
				</span>
			),
		},
		{
			heading: 'environment-type',
			key: 'environment-type',
			render: (row) => translate(row.environmentType),
		},
		{
			heading: 'start-date-exp-date',
			key: 'start-date-exp-date',
			render: (row) => (
				<span className="list-card-status">
					<ClayTooltipProvider>
						<span
							className="list-card-status-dot"
							data-tooltip-align="top"
							style={{
								backgroundColor: getStatusColor(row.status),
							}}
							title={translate(row.status)}
						/>
					</ClayTooltipProvider>

					<span className="d-flex flex-column">
						<span>{`${row.startDate} -`}</span>

						<span>{row.expirationDate}</span>
					</span>
				</span>
			),
		},
		{
			key: 'action',
			render: (row) => (
				<KebabActions
					onDeactivate={() => handleDeactivate(row)}
					onDownload={() => handleDownload(row)}
					onReactivate={() => handleReactivate(row)}
					onRenew={() => handleRenew(row)}
					onView={() => navigate(row.id)}
					row={row}
				/>
			),
			width: '1%',
		},
	];

	const filters: ListFilter<ProjectActivationKey>[] = [
		{
			key: 'environmentType',
			label: 'environment-type',
			matches: (row, values) => values.includes(row.environmentType),
			options: [
				{label: translate('non-production'), value: 'non-production'},
				{label: translate('production'), value: 'production'},
			],
		},
		{
			formatValue: formatDateBound,
			key: 'startDate',
			label: 'start-date',
			matches: (row, values) =>
				matchesDateBound(row.startDateValue, values),
			variant: 'date-range',
		},
		{
			formatValue: formatDateBound,
			key: 'endDate',
			label: 'end-date',
			matches: (row, values) =>
				matchesDateBound(row.expirationDateValue, values),
			variant: 'date-range',
		},
	];

	return (
		<Page
			description={i18n.translate(
				'manage-the-activation-keys-within-your-project'
			)}
			pageRendererProps={{isLoading: loading}}
			title={i18n.translate('activation-keys')}
		>
			<FilterableListCard
				action={
					<Button displayType="primary" onClick={handleNewKey}>
						{translate('new-key')}
					</Button>
				}
				columns={columns}
				emptyLabel="no-activation-keys-yet"
				filters={filters}
				items={activationKeys}
				matchesSearch={matchesSearch}
				onItemClick={(row) => navigate(row.id)}
				rowKey={(row) => row.id}
			/>
		</Page>
	);
}
