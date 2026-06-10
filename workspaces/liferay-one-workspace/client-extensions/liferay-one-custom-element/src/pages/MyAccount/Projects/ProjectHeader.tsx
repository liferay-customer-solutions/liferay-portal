/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ReactNode} from 'react';

import {useProject} from '../../../context/ProjectContext';
import i18n from '../../../i18n';
import {getProject} from './projects';

const LABEL_COLOR = 'var(--color-neutral-7)';
const VALUE_COLOR = 'var(--color-neutral-10)';

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
				borderLeft: first
					? undefined
					: '1px solid var(--color-neutral-2)',
				gap: 'var(--spacer-1)',
				padding: first ? '0 var(--spacer-4) 0 0' : '0 var(--spacer-4)',
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
				border: '1px solid var(--color-neutral-2)',
				borderRadius: 'var(--border-radius-lg, 0.625rem)',
				padding: 'var(--spacer-3) var(--spacer-2)',
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
					style={{gap: 'var(--spacer-2)'}}
				>
					<a
						className="text-decoration-none"
						href="#"
						style={{color: 'var(--color-brand-primary)'}}
					>
						{i18n.translate('order-form')}
					</a>

					<span style={{color: LABEL_COLOR}}>·</span>

					<a
						className="text-decoration-none"
						href="#"
						style={{color: 'var(--color-brand-primary)'}}
					>
						{i18n.translate('eula')}
					</a>
				</span>
			</Section>
		</div>
	);
}
