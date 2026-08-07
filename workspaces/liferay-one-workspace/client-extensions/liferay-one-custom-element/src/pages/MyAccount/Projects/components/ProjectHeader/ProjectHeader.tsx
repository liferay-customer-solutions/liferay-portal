/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {format} from 'date-fns';
import {useState} from 'react';
import contractTermIconUrl from '~/assets/icons/contract_term_icon.svg';
import EntitySelector, {
	SelectorItem,
} from '~/components/EntitySelector/EntitySelector';
import {useProject} from '~/context/ProjectContext';
import {
	resolveDefaultContractERC,
	useProjectCommerce,
} from '~/hooks/useProjectCommerce';
import i18n from '~/i18n';
import {ONE_TIME_PURCHASES} from '~/pages/MyAccount/Projects/projects';

function formatTermRange(startDate?: string, endDate?: string): string {
	if (!startDate || !endDate) {
		return '-';
	}

	return `${format(new Date(startDate), 'MM.dd.yyyy')} - ${format(
		new Date(endDate),
		'MM.dd.yyyy'
	)}`;
}

export default function ProjectHeader() {
	const {projectId, selectedContractERC, setSelectedContractERC} =
		useProject();

	const {contracts} = useProjectCommerce(projectId);

	const [searchValue, setSearchValue] = useState('');

	const selectedContractExists = contracts.some(
		(contract) => contract.externalReferenceCode === selectedContractERC
	);

	const resolvedContractERC = selectedContractExists
		? selectedContractERC
		: resolveDefaultContractERC(contracts);

	const selectedContract = contracts.find(
		(contract) => contract.externalReferenceCode === resolvedContractERC
	);

	const items: SelectorItem[] = contracts
		.filter((contract) =>
			contract.externalReferenceCode
				.toLowerCase()
				.includes(searchValue.trim().toLowerCase())
		)
		.map((contract) => ({
			id: contract.externalReferenceCode,
			name: contract.name,
			subtitle:
				contract.externalReferenceCode === ONE_TIME_PURCHASES
					? i18n.translate('no-contract-linked')
					: formatTermRange(contract.startDate, contract.endDate),
		}));

	const oneTimeSelected =
		selectedContract?.externalReferenceCode === ONE_TIME_PURCHASES;

	const triggerName = oneTimeSelected
		? selectedContract.name
		: formatTermRange(
				selectedContract?.startDate,
				selectedContract?.endDate
			);

	const triggerSubtitle = oneTimeSelected
		? i18n.translate('no-contract-linked')
		: undefined;

	const handleSelect = (contractERC: string) => {
		setSearchValue('');

		setSelectedContractERC(contractERC);
	};

	const readOnly = contracts.length <= 1;

	return (
		<div
			className="d-flex flex-wrap mb-3"
			style={
				readOnly
					? undefined
					: {
							backgroundColor: 'var(--color-neutral-1)',
							border: '1px solid var(--color-neutral-2)',
							borderRadius: 'var(--border-radius-lg, 0.625rem)',
							padding: 'var(--spacer-3) var(--spacer-2)',
							width: 'fit-content',
						}
			}
		>
			<EntitySelector
				ariaLabel={i18n.translate('select-contract')}
				items={items}
				label={i18n.translate('contract-term')}
				name={triggerName}
				onSearchChange={setSearchValue}
				onSelect={handleSelect}
				readOnly={readOnly}
				searchValue={searchValue}
				selectedId={resolvedContractERC}
				subtitle={triggerSubtitle}
				triggerIcon={<img alt="" src={contractTermIconUrl} />}
				variant="rich"
			/>
		</div>
	);
}
