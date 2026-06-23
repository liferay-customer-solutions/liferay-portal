/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import {ReactNode} from 'react';
import {NavLink} from 'react-router-dom';
import applicationsIconUrl from '~/assets/icons/applications.svg';
import productsIconUrl from '~/assets/icons/products.svg';
import unionIconUrl from '~/assets/icons/union.svg';

export type NavItem = {
	children?: NavItem[];
	end?: boolean;
	icon?: string;
	label: string;
	path: string;
};

const ACTIVE_BG = 'var(--color-action-primary-active-lighten)';

const CUSTOM_ICON_SPRITEMAPS: Record<string, string> = {
	applications: applicationsIconUrl,
	products: productsIconUrl,
	union: unionIconUrl,
};

const SIDEBAR_BG = 'var(--color-neutral-1)';
const TEXT_ACTIVE = 'var(--color-action-primary-active)';
const TEXT_INACTIVE = 'var(--color-neutral-10)';
const TEXT_SECTION = 'var(--color-neutral-7)';

type SideNavItemProps = {
	depth: number;
	item: NavItem;
};

function SideNavItem({depth, item}: SideNavItemProps) {
	const hasChildren = Boolean(item.children && item.children.length);
	const icon = item.icon;
	const customSpritemap = icon ? CUSTOM_ICON_SPRITEMAPS[icon] : undefined;

	return (
		<li>
			<NavLink
				className="align-items-center d-flex text-decoration-none w-100"
				end={item.end ?? !hasChildren}
				style={({isActive}) => ({
					backgroundColor: isActive ? ACTIVE_BG : 'transparent',
					borderRadius: '0.375rem',
					color: isActive ? TEXT_ACTIVE : TEXT_INACTIVE,
					fontSize: '1rem',
					fontWeight: 600,
					gap: '0.75rem',
					lineHeight: '1.5rem',
					padding: `0.75rem 0.75rem 0.75rem ${0.75 + depth * 0.75}rem`,
				})}
				to={item.path}
			>
				{({isActive}) => (
					<>
						{icon && (
							<ClayIcon
								spritemap={customSpritemap}
								style={{
									flexShrink: 0,
									opacity: isActive ? 1 : 0.5,
								}}
								symbol={icon}
							/>
						)}

						<span style={{flex: '1 0 0', textAlign: 'left'}}>
							{item.label}
						</span>
					</>
				)}
			</NavLink>

			{hasChildren && (
				<ul className="list-unstyled m-0">
					{item.children?.map((child) => (
						<SideNavItem
							depth={depth + 1}
							item={child}
							key={child.path}
						/>
					))}
				</ul>
			)}
		</li>
	);
}

type SideNavProps = {
	header?: ReactNode;
	items: NavItem[];
	title?: string;
};

export default function SideNav({header, items, title}: SideNavProps) {
	return (
		<nav
			className="align-self-start d-flex flex-column flex-shrink-0"
			style={{gap: '0.75rem', width: '18rem'}}
		>
			{header && (
				<div
					style={{
						backgroundColor: SIDEBAR_BG,
						borderRadius: 'var(--border-radius-lg, 0.625rem)',
						padding: '0.75rem',
					}}
				>
					{header}
				</div>
			)}

			<div
				className="d-flex flex-column overflow-auto"
				style={{
					backgroundColor: SIDEBAR_BG,
					borderRadius: 'var(--border-radius-lg, 0.625rem)',
					padding: 'var(--spacer-2)',
				}}
			>
				{title && (
					<div
						className="font-weight-bold pb-2 px-3 text-uppercase"
						style={{
							color: TEXT_SECTION,
							fontSize: '0.625rem',
							letterSpacing: '0.1em',
						}}
					>
						{title}
					</div>
				)}

				<ul className="d-flex flex-column flex-fill gap-2 list-unstyled m-0">
					{items.map((item) => (
						<SideNavItem depth={0} item={item} key={item.path} />
					))}
				</ul>
			</div>
		</nav>
	);
}
