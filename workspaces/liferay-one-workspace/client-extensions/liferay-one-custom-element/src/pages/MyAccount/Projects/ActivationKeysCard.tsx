/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';

import Button from '../../../components/Button/Button';
import {Word, translate} from '../../../i18n';
import FilterableListCard, {ListColumn, ListFilter} from './FilterableListCard';
import {ACTIVATION_KEYS, ActivationKey, getStatusColor} from './tabData';

const BADGE_COLORS: {[key: string]: {color: string; icon: string}} = {
	'new-activation-key': {color: 'var(--color-success)', icon: 'check-circle'},
	'to-be-renewed': {color: 'var(--color-warning)', icon: 'warning-full'},
};

function matchesSearch(activationKey: ActivationKey, search: string): boolean {
	return (
		activationKey.name.toLowerCase().includes(search) ||
		activationKey.domain.toLowerCase().includes(search)
	);
}

export default function ActivationKeysCard() {
	const filters = useMemo<ListFilter<ActivationKey>[]>(() => {
		const statuses = Array.from(
			new Set(
				ACTIVATION_KEYS.map((activationKey) => activationKey.status)
			)
		).sort();

		return [
			{
				key: 'status',
				label: 'status',
				matches: (activationKey, values) =>
					values.includes(activationKey.status),
				options: statuses.map((status) => ({
					label: translate(status as Word),
					value: status,
				})),
			},
		];
	}, []);

	const columns: ListColumn<ActivationKey>[] = [
		{
			heading: 'activation-key',
			key: 'activation-key',
			render: (activationKey) => (
				<span className="d-flex flex-column">
					<span style={{fontWeight: 600}}>{activationKey.name}</span>

					{activationKey.badge && (
						<span
							className="align-items-center d-flex"
							style={{
								color: BADGE_COLORS[activationKey.badge].color,
								fontSize: '13px',
								gap: '0.25rem',
							}}
						>
							<ClayIcon
								symbol={BADGE_COLORS[activationKey.badge].icon}
							/>

							{translate(activationKey.badge)}
						</span>
					)}
				</span>
			),
		},
		{
			heading: 'domain',
			key: 'domain',
			render: (activationKey) => (
				<span className="d-flex flex-column">
					<span style={{fontWeight: 600}}>{translate('domain')}</span>

					<span className="list-card-subtext">
						{activationKey.domain}
					</span>
				</span>
			),
		},
		{
			heading: 'start-date-exp-date',
			key: 'start-date-exp-date',
			render: (activationKey) => (
				<span className="d-flex flex-column">
					<span>{`${activationKey.startDate} -`}</span>

					<span>{activationKey.expirationDate}</span>
				</span>
			),
		},
		{
			heading: 'status',
			key: 'status',
			render: (activationKey) => (
				<span className="list-card-status">
					<span
						className="list-card-status-dot"
						style={{
							backgroundColor: getStatusColor(
								activationKey.status
							),
						}}
					/>

					{translate(activationKey.status as Word)}
				</span>
			),
		},
		{
			key: 'renew',
			render: () => (
				<ClayButton
					borderless
					className="text-neutral-7"
					displayType="unstyled"
					onClick={(event) => event.stopPropagation()}
				>
					{translate('renew')}
				</ClayButton>
			),
		},
		{
			key: 'download',
			render: (activationKey) => (
				<Button
					disabled={activationKey.status === 'expired'}
					displayType="secondary"
					onClick={(event) => event.stopPropagation()}
				>
					{translate('download')}
				</Button>
			),
		},
	];

	return (
		<FilterableListCard
			action={
				<Button displayType="primary" prependIcon="plus">
					{translate('new-key')}
				</Button>
			}
			columns={columns}
			emptyLabel="no-activation-keys-yet"
			filters={filters}
			items={ACTIVATION_KEYS}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(activationKey) => activationKey.id}
			title="activation-keys-list"
		/>
	);
}
