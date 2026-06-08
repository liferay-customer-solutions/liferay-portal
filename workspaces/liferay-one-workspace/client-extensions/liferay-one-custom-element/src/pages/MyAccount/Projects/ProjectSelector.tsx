/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayDropDown, {Align} from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {useState} from 'react';
import {useNavigate} from 'react-router-dom';

import projectIconUrl from '../../../assets/icons/project.svg';
import {useProject} from '../../../context/ProjectContext';
import i18n from '../../../i18n';
import {PROJECTS, getProject} from './projects';

export default function ProjectSelector() {
	const {projectId, setProjectId} = useProject();
	const navigate = useNavigate();
	const [active, setActive] = useState(false);

	const project = getProject(projectId);

	function handleSelect(id: string) {
		setActive(false);

		if (id !== projectId) {
			setProjectId(id);

			navigate('/project/products');
		}
	}

	return (
		<ClayDropDown
			active={active}
			alignmentPosition={Align.BottomLeft}
			onActiveChange={setActive}
			trigger={
				<button
					aria-label={i18n.translate('select-project')}
					className="align-items-center bg-transparent border-0 d-flex p-0 text-left w-100"
					style={{gap: '0.75rem'}}
					type="button"
				>
					<span
						className="align-items-center d-flex justify-content-center"
						style={{
							background:
								'linear-gradient(135deg, #E8EDFB, #D3E0FB)',
							borderRadius: '0.625rem',
							color: '#1B5FE0',
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

					<span
						className="d-flex flex-column flex-fill"
						style={{minWidth: 0}}
					>
						<span
							style={{
								color: '#6B6C7E',
								fontSize: '0.6875rem',
								fontWeight: 600,
								letterSpacing: '0.06em',
								textTransform: 'uppercase',
							}}
						>
							{i18n.translate('project')} ({PROJECTS.length})
						</span>

						<span
							className="align-items-center d-flex"
							style={{gap: '0.25rem'}}
						>
							<span
								className="text-truncate"
								style={{
									color: '#272833',
									fontSize: '0.9375rem',
									fontWeight: 700,
									minWidth: 0,
								}}
							>
								{project?.name ?? projectId}
							</span>

							<ClayIcon
								style={{color: '#6B6C7E', flexShrink: 0}}
								symbol="caret-bottom"
							/>
						</span>

						{project?.status && (
							<span
								style={{
									alignSelf: 'flex-start',
									backgroundColor: '#DBF0DC',
									borderRadius: '0.25rem',
									color: '#1F7A3D',
									fontSize: '0.6875rem',
									fontWeight: 700,
									marginTop: '0.25rem',
									padding: '0.0625rem 0.5rem',
								}}
							>
								{i18n.translate('active')}
							</span>
						)}
					</span>
				</button>
			}
		>
			<ClayDropDown.ItemList>
				{PROJECTS.map((option) => (
					<ClayDropDown.Item
						active={option.id === projectId}
						key={option.id}
						onClick={() => handleSelect(option.id)}
					>
						{option.name}
					</ClayDropDown.Item>
				))}
			</ClayDropDown.ItemList>
		</ClayDropDown>
	);
}
