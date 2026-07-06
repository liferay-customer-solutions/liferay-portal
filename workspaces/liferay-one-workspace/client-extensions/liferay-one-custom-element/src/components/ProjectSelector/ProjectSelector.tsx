/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import projectIconUrl from '~/assets/icons/project.svg';
import EntitySelector, {
	SelectorItem,
} from '~/components/EntitySelector/EntitySelector';
import {translate} from '~/i18n';

import type {UserProject} from '~/pages/MyAccount/Projects/types';

type ProjectSelectorProps = {
	emptyLabel?: string;
	loading?: boolean;
	onSelect: (projectERC: string) => void;
	projects: UserProject[];
	readOnly?: boolean;
	selectedProjectERC?: string;
	showProjectCount?: boolean;
};

const ProjectSelector = ({
	emptyLabel,
	loading,
	onSelect,
	projects,
	readOnly,
	selectedProjectERC,
	showProjectCount = false,
}: ProjectSelectorProps) => {
	const [searchValue, setSearchValue] = useState('');

	const currentProject = projects.find(
		(project) => project.externalReferenceCode === selectedProjectERC
	);

	const items: SelectorItem[] = projects
		.filter((project) =>
			project.name
				.toLowerCase()
				.includes(searchValue.trim().toLowerCase())
		)
		.map((project) => ({
			id: project.externalReferenceCode,
			name: project.name,
			subtitle: project.unassigned
				? translate('no-project-linked')
				: undefined,
		}));

	const handleSelect = (id: string) => {
		setSearchValue('');

		onSelect(id);
	};

	const projectCount = projects.filter(
		(project) => !project.unassigned
	).length;

	return (
		<EntitySelector
			ariaLabel={translate('select-project')}
			badge={
				currentProject?.unassigned
					? translate('no-project-linked')
					: undefined
			}
			emptyLabel={emptyLabel}
			items={items}
			label={
				showProjectCount
					? `${translate('project')} (${projectCount})`
					: translate('project')
			}
			loading={loading}
			name={currentProject?.name ?? ''}
			onSearchChange={setSearchValue}
			onSelect={handleSelect}
			readOnly={readOnly ?? projects.length === 1}
			searchValue={searchValue}
			selectedId={selectedProjectERC}
			triggerIcon={
				<span
					className="align-items-center d-flex justify-content-center"
					style={{
						background:
							'linear-gradient(135deg, var(--color-action-primary-active-lighten), var(--color-brand-primary-lighten-5))',
						borderRadius: 'var(--border-radius-lg, 0.625rem)',
						color: 'var(--color-brand-primary)',
						flexShrink: 0,
						height: '2.75rem',
						width: '2.75rem',
					}}
				>
					<ClayIcon
						spritemap={projectIconUrl}
						style={{height: '1.5rem', width: '1.5rem'}}
						symbol="project"
					/>
				</span>
			}
			variant="rich"
		/>
	);
};

export default ProjectSelector;
