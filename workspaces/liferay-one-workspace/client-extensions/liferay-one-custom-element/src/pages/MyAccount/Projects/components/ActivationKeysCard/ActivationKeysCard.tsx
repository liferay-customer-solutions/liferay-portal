/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useMemo} from 'react';
import newKeyIconUrl from '~/assets/icons/new_key_icon.svg';
import renewedKeyIconUrl from '~/assets/icons/renewed_key_icon.svg';
import Button from '~/components/Button/Button';
import {
	ProjectActivationKey,
	useProjectActivationKeys,
} from '~/hooks/useProjectActivationKeys';
import {Word, translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import FilterableListCard, {
	ListColumn,
	ListFilter,
} from '../FilterableListCard/FilterableListCard';

const BADGE_COLORS: {[key: string]: {color: string; icon: string}} = {
	'new-activation-key': {color: 'var(--color-state-success)', icon: newKeyIconUrl},
	'to-be-renewed': {color: '#9D4C00', icon: renewedKeyIconUrl},
};

type ActivationKeysCardProps = {
	productName?: string;
};

function matchesSearch(
	activationKey: ProjectActivationKey,
	search: string
): boolean {
	return (
		activationKey.name.toLowerCase().includes(search) ||
		activationKey.domain.toLowerCase().includes(search)
	);
}

export default function ActivationKeysCard({
	productName,
}: ActivationKeysCardProps) {
	const {activationKeys} = useProjectActivationKeys(productName);

	const filters = useMemo<ListFilter<ProjectActivationKey>[]>(() => {
		const statuses = Array.from(
			new Set(activationKeys.map((activationKey) => activationKey.status))
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
	}, [activationKeys]);

	const columns: ListColumn<ProjectActivationKey>[] = [
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
							<img
								alt=""
								height={16}
								src={BADGE_COLORS[activationKey.badge].icon}
								width={16}
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
					<span style={{whiteSpace: 'nowrap'}}>
						{`${activationKey.startDate} -`}
					</span>

					<span style={{whiteSpace: 'nowrap'}}>
						{activationKey.expirationDate}
					</span>
				</span>
			),
			width: '180px',
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
				<Button displayType="primary">
					{translate('new-key')}
				</Button>
			}
			columns={columns}
			emptyLabel="no-activation-keys-yet"
			filters={filters}
			items={activationKeys}
			matchesSearch={matchesSearch}
			onItemClick={() => {}}
			rowKey={(activationKey) => activationKey.id}
			title="activation-keys-list"
		/>
	);
}
