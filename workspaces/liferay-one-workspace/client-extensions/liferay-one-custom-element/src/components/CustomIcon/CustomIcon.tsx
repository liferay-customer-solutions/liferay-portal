/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {ComponentProps} from 'react';
import applicationsSprite from '~/assets/icons/applications.svg?raw';
import keyHorizontalSprite from '~/assets/icons/key_horizontal.svg?raw';
import productsSprite from '~/assets/icons/products.svg?raw';
import projectSprite from '~/assets/icons/project.svg?raw';
import unionSprite from '~/assets/icons/union.svg?raw';

const CUSTOM_ICON_SPRITES = [
	applicationsSprite,
	keyHorizontalSprite,
	productsSprite,
	projectSprite,
	unionSprite,
];

const CUSTOM_ICON_SYMBOLS = new Set([
	'applications',
	'key-horizontal',
	'products',
	'project',
	'union',
]);

const SPRITE_ELEMENT_ID = 'liferay-one-custom-icon-sprite';

function injectCustomIconSprite() {
	if (
		typeof document === 'undefined' ||
		document.getElementById(SPRITE_ELEMENT_ID)
	) {
		return;
	}

	const container = document.createElement('div');

	container.id = SPRITE_ELEMENT_ID;
	container.setAttribute('aria-hidden', 'true');
	container.style.display = 'none';
	container.innerHTML = CUSTOM_ICON_SPRITES.map((markup) =>
		markup.replace(/<\?xml[^>]*\?>/, '')
	).join('');

	document.body.appendChild(container);
}

type CustomIconProps = ComponentProps<typeof ClayIcon>;

export default function CustomIcon({
	className,
	symbol,
	...otherProps
}: CustomIconProps) {
	if (!CUSTOM_ICON_SYMBOLS.has(symbol)) {
		return (
			<ClayIcon className={className} symbol={symbol} {...otherProps} />
		);
	}

	injectCustomIconSprite();

	const svgProps = {...otherProps};

	delete svgProps.spritemap;

	return (
		<svg
			className={classNames('lexicon-icon', className)}
			role="presentation"
			{...svgProps}
		>
			<use href={`#${symbol}`} />
		</svg>
	);
}
