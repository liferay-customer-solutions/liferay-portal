/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {Link, useLocation, useParams} from 'react-router-dom';
import i18n, {Word} from '~/i18n';

type Crumb = {
	href?: string;
	label: Word;
	to?: string;
};

const LINK_COLOR = 'var(--color-neutral-7)';
const SEPARATOR_COLOR = 'var(--color-neutral-4)';
const TEXT_COLOR = 'var(--color-neutral-10)';

function getCurrentLabel(pathname: string): Word {
	if (pathname.includes('/orders')) {
		return 'orders';
	}

	if (pathname.includes('/account-details')) {
		return 'account-details';
	}

	if (pathname.includes('/account-members')) {
		return 'account-members';
	}

	return 'project';
}

export default function Breadcrumb() {
	const {pathname} = useLocation();
	const {accountERC} = useParams();

	const trail: Crumb[] = [
		{href: '/', label: 'home'},
		{label: 'my-account', to: accountERC ? `/${accountERC}` : '/'},
		{label: getCurrentLabel(pathname)},
	];

	return (
		<nav aria-label={i18n.translate('breadcrumb')} className="mb-3">
			<ol
				className="align-items-center d-flex flex-wrap list-unstyled m-0"
				style={{gap: 'var(--spacer-2)'}}
			>
				{trail.map((crumb, index) => {
					const isLast = index === trail.length - 1;
					const text = i18n.translate(crumb.label);

					return (
						<li
							className="align-items-center d-flex"
							key={crumb.label}
							style={{gap: 'var(--spacer-2)'}}
						>
							{isLast ? (
								<span
									style={{
										color: TEXT_COLOR,
										fontWeight: 600,
									}}
								>
									{text}
								</span>
							) : crumb.to ? (
								<Link
									style={{
										color: LINK_COLOR,
										textDecoration: 'none',
									}}
									to={crumb.to}
								>
									{text}
								</Link>
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
