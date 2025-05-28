/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useState} from 'react';

import './TileCard.css';

interface TileCardProps {
	hoverSvgIcon?: JSX.Element;
	link: string;
	subtitle: string;
	svgIcon: JSX.Element;
	title: string;
}

const TileCard = ({
	hoverSvgIcon,
	link,
	subtitle,
	svgIcon,
	title,
}: TileCardProps) => {
	const [isHovered, setIsHovered] = useState(false);

	return (
		<a
			className="tile-card-link"
			href={link}
			onMouseEnter={() => setIsHovered(true)}
			onMouseLeave={() => setIsHovered(false)}
		>
			<div className="tile-card-wrapper">
				<div className="align-items-center d-flex flex-column p-4 tile-card-container">
					<div className="tile-card-icon">
						{isHovered && hoverSvgIcon ? hoverSvgIcon : svgIcon}
					</div>

					<div className="font-weight-bold pt-4 text-center tile-card-title">
						{title}
					</div>

					<div className="text-center tile-card-subtitle">
						{subtitle}
					</div>
				</div>
			</div>
		</a>
	);
};

export default TileCard;
