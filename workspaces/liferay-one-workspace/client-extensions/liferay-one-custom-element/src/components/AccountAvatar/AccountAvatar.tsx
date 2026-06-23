/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';

type AccountAvatarProps = {
	logoURL?: string;
	size?: number;
	type?: string;
};

export default function AccountAvatar({
	logoURL,
	size = 32,
	type,
}: AccountAvatarProps) {
	const hasLogo = Boolean(logoURL) && !logoURL?.includes('img_id=0');

	if (hasLogo) {
		return (
			<img
				alt=""
				className="rounded-circle"
				src={logoURL}
				style={{
					flexShrink: 0,
					height: size,
					objectFit: 'cover',
					width: size,
				}}
			/>
		);
	}

	return (
		<span
			className="align-items-center bg-light d-flex justify-content-center rounded-circle text-neutral-6"
			style={{flexShrink: 0, height: size, width: size}}
		>
			<ClayIcon
				symbol={
					type?.toLowerCase() === 'business' ? 'briefcase' : 'user'
				}
			/>
		</span>
	);
}
