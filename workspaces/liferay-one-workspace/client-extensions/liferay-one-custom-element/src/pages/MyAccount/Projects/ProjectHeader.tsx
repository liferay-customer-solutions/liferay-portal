/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import {useProject} from '../../../context/ProjectContext';
import i18n from '../../../i18n';
import {getProject} from './projects';

const LABEL_COLOR = '#6B6C7E';
const VALUE_COLOR = '#272833';

type SectionProps = {
	children: ReactNode;
	first?: boolean;
	label: string;
};

function Section({children, first, label}: SectionProps) {
	return (
		<div
			className="d-flex flex-column"
			style={{
				borderLeft: first ? undefined : '1px solid #E7E7ED',
				gap: '0.25rem',
				padding: first ? '0 1.5rem 0 0' : '0 1.5rem',
			}}
		>
			<span
				style={{
					color: LABEL_COLOR,
					fontSize: '0.6875rem',
					fontWeight: 600,
					letterSpacing: '0.06em',
					textTransform: 'uppercase',
				}}
			>
				{label}
			</span>

			<span
				style={{
					color: VALUE_COLOR,
					fontSize: '0.9375rem',
					fontWeight: 600,
				}}
			>
				{children}
			</span>
		</div>
	);
}

export default function ProjectHeader() {
	const {projectId} = useProject();

	const project = getProject(projectId);

	return (
		<div
			className="align-items-center d-flex flex-wrap justify-content-between mb-3"
			style={{
				border: '1px solid #E7E7ED',
				borderRadius: '0.625rem',
				padding: '1rem 0.5rem',
			}}
		>
			<Section first label={i18n.translate('project-term')}>
				{project?.termRange ?? '-'}
			</Section>

			<Section label={i18n.translate('term')}>
				{project ? i18n.translate(project.termType as 'annual') : '-'}
			</Section>

			<Section label={i18n.translate('agreements')}>
				<span
					className="align-items-center d-flex"
					style={{gap: '0.5rem'}}
				>
					<a
						className="text-decoration-none"
						href="#"
						style={{color: '#0B5FFF'}}
					>
						{i18n.translate('order-form')}
					</a>

					<span style={{color: LABEL_COLOR}}>·</span>

					<a
						className="text-decoration-none"
						href="#"
						style={{color: '#0B5FFF'}}
					>
						{i18n.translate('eula')}
					</a>
				</span>
			</Section>
		</div>
	);
}
