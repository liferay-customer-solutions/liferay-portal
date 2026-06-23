/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {useNavigate, useParams} from 'react-router-dom';
import projectIconUrl from '~/assets/icons/project.svg';
import EntitySelector, {
	SelectorItem,
} from '~/components/EntitySelector/EntitySelector';
import {useProject} from '~/context/ProjectContext';
import i18n from '~/i18n';

export default function ProjectSelector() {
	const {loading, projectId, projects} = useProject();
	const {accountERC} = useParams();
	const navigate = useNavigate();
	const [searchValue, setSearchValue] = useState('');

	const project = projects.find(
		(option) => option.externalReferenceCode === projectId
	);

	function handleSelect(id: string) {
		setSearchValue('');

		if (id !== projectId) {
			navigate(`/${accountERC}/project/${id}/products`);
		}
	}

	const items: SelectorItem[] = projects
		.filter((option) =>
			option.name.toLowerCase().includes(searchValue.trim().toLowerCase())
		)
		.map((option) => ({
			id: option.externalReferenceCode,
			name: option.name,
			subtitle: option.unassigned
				? i18n.translate('no-project-linked')
				: undefined,
		}));

	const projectCount = projects.filter((option) => !option.unassigned).length;

	return (
		<EntitySelector
			ariaLabel={i18n.translate('select-project')}
			badge={
				project?.unassigned
					? i18n.translate('no-project-linked')
					: undefined
			}
			emptyLabel="no-projects-yet"
			items={items}
			label={`${i18n.translate('project')} (${projectCount})`}
			loading={loading}
			name={project?.name ?? ''}
			onSearchChange={setSearchValue}
			onSelect={handleSelect}
			searchValue={searchValue}
			selectedId={projectId}
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
}
