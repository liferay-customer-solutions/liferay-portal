/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown from '@clayui/drop-down';
import Button from '~/components/Button/Button';
import {
	ProjectActivationKey,
	useProjectActivationKeys,
} from '~/hooks/useProjectActivationKeys';
import {Word, translate} from '~/i18n';
import {getStatusColor} from '~/pages/MyAccount/Projects/utils/getStatusColor';

import FilterableListCard, {
	ListColumn,
} from '../FilterableListCard/FilterableListCard';

type LicensesTableVariant = 'app-licenses' | 'licenses';

type LicensesTableProps = {
	productName?: string;
	variant: LicensesTableVariant;
};

const DEFAULT_KEY_TYPE = 'On-Premise';

function KebabActions({activationKey}: {activationKey: ProjectActivationKey}) {
	const isExpired = activationKey.status === 'expired';

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
				<ClayDropDown.Item>
					{translate('view-license-details')}
				</ClayDropDown.Item>

				<ClayDropDown.Item disabled={isExpired}>
					{translate('download-license-key')}
				</ClayDropDown.Item>

				<ClayDropDown.Item className="text-danger">
					{translate('deactivate-license-key')}
				</ClayDropDown.Item>
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}

export default function LicensesTable({
	productName,
	variant,
}: LicensesTableProps) {
	const {activationKeys} = useProjectActivationKeys(productName);

	const columns: ListColumn<ProjectActivationKey>[] = [
		{
			heading: 'environment',
			key: 'environment',
			render: (activationKey) => (
				<span style={{fontWeight: 600}}>{activationKey.name}</span>
			),
		},
		{
			heading: 'key-type',
			key: 'key-type',
			render: (activationKey) => (
				<span className="d-flex flex-column">
					<span style={{fontWeight: 600}}>{DEFAULT_KEY_TYPE}</span>

					<span className="list-card-subtext">
						{activationKey.domain || '-'}
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
			key: 'action',
			render: (activationKey) =>
				variant === 'app-licenses' ? (
					<KebabActions activationKey={activationKey} />
				) : (
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
			columns={columns}
			emptyLabel="no-activation-keys-yet"
			hideToolbar
			items={activationKeys}
			onItemClick={() => {}}
			rowKey={(activationKey) => activationKey.id}
			title={
				variant === 'app-licenses' ? 'licenses-list' : 'activation-keys'
			}
		/>
	);
}
