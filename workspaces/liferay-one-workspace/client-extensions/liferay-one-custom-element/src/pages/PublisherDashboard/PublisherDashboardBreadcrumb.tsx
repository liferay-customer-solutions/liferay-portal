/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';

import i18n, {Word} from '../../i18n';

type Crumb = {
	href?: string;
	label: Word;
};

const LINK_COLOR = 'var(--color-neutral-7)';
const SEPARATOR_COLOR = 'var(--color-neutral-4)';
const TEXT_COLOR = 'var(--color-neutral-10)';

const TRAIL: Crumb[] = [
	{href: '/', label: 'home'},
	{href: '/web/one/my-account', label: 'my-accounts'},
	{label: 'publisher-dashboard'},
];

export default function PublisherDashboardBreadcrumb() {
	return (
		<nav aria-label={i18n.translate('breadcrumb')} className="mb-3">
			<ol
				className="align-items-center d-flex flex-wrap list-unstyled m-0"
				style={{gap: 'var(--spacer-2)'}}
			>
				{TRAIL.map((crumb, index) => {
					const isLast = index === TRAIL.length - 1;
					const text = i18n.translate(crumb.label);

					return (
						<li
							className="align-items-center d-flex"
							key={crumb.label}
							style={{gap: 'var(--spacer-2)'}}
						>
							{isLast ? (
								<span style={{color: TEXT_COLOR, fontWeight: 600}}>
									{text}
								</span>
							) : (
								<a
									href={crumb.href}
									style={{
										color: LINK_COLOR,
										textDecoration: 'none',
									}}
								>
									{text}
								</a>
							)}

							{!isLast && (
								<ClayIcon
									style={{
										color: SEPARATOR_COLOR,
										fontSize: '0.5em',
										marginTop: '0',
									}}
									symbol="angle-right"
								/>
							)}
						</li>
					);
				})}
			</ol>
		</nav>
	);
}
