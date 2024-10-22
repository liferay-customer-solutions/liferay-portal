/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Nav from '@clayui/nav';
import {memo, useState} from 'react';

import Skeleton from '../Skeleton';

interface IProps {
	disabled?: boolean;
	items?: {
		key: string;
		label: string;
	}[];
	loading?: boolean;
	maxItemsLoading?: number;
	onSelect: (index: number | {key: string; label: string}) => void;
	selectedIndex?: number;
}

const NavSegment: React.FC<IProps> = ({
	disabled,
	items,
	loading,
	maxItemsLoading = 3,
	onSelect,
	selectedIndex,
}) => {
	const [currentIndex, setCurrentIndex] = useState(selectedIndex || 0);

	const handleOnClick = (index: number) => {
		if (index !== currentIndex) {
			setCurrentIndex(index);

			if (selectedIndex !== undefined) {
				onSelect(index);

				return;
			}

			items && onSelect(items[index]);
		}
	};

	const getNavItemsSkeleton = () =>
		[...new Array(maxItemsLoading)].map((_, index) => (
			<Nav.Item key={index}>
				<Nav.Link>
					<Skeleton align="left" height={20} width={100} />
				</Nav.Link>
			</Nav.Item>
		));

	const getNavItems = () =>
		items?.map((item, index) => (
			<Nav.Item
				key={`${item.key}-${index}`}
				onClick={() => handleOnClick(index)}
			>
				<Nav.Link
					active={index === currentIndex}
					className="cp-nav-link text-neutral-10"
					disabled={disabled}
				>
					{item.label}
				</Nav.Link>
			</Nav.Item>
		));

	return (
		<Nav className="nav-segment">
			{loading ? getNavItemsSkeleton() : getNavItems()}
		</Nav>
	);
};

export default memo(NavSegment);
