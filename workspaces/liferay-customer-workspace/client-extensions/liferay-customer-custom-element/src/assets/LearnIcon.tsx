/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {SVGProps} from 'react';
import {JSX} from 'react/jsx-runtime';

const LearnIcon = (
	props: JSX.IntrinsicAttributes & SVGProps<SVGSVGElement>
) => (
	<svg
		fill="none"
		height="48"
		viewBox="0 0 48 48"
		width="48"
		xmlns="http://www.w3.org/2000/svg"
		{...props}
	>
		<path
			d="M46.2198 17.331L23.9998 30.671L1.77979 17.331L23.9998 4.00098L46.2198 17.331Z"
			fill="#0B5FFF"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
		<path
			d="M39.5601 21.3809V32.8909"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
		<path
			d="M39.5599 32.8909V35.1109C39.5599 40.0209 32.5999 44.0009 23.9999 44.0009C15.3999 44.0009 8.43994 40.0209 8.43994 35.1109V21.3809"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
		<path
			d="M32.8901 35.1109V26.2209"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
	</svg>
);

export {LearnIcon};
