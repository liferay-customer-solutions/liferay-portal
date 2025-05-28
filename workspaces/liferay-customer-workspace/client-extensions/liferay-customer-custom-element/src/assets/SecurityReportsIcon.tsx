/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {SVGProps} from 'react';
import {JSX} from 'react/jsx-runtime';

const SecurityReportsIcon = (
	props: JSX.IntrinsicAttributes & SVGProps<SVGSVGElement>
) => (
	<svg
		fill="none"
		height="48"
		viewBox="0 0 38 48"
		width="38"
		xmlns="http://www.w3.org/2000/svg"
		{...props}
	>
		<g style={{mixBlendMode: 'multiply'}}>
			<path
				d="M19 46.5149H19.0225C36.8086 40.2335 37 20.196 37 10.0647C31.0788 10.5938 22.7711 8.01589 19.0112 1.48682H18.9887C15.2289 8.01589 6.9212 10.605 1 10.0647C1 20.196 1.19137 40.2223 18.9775 46.5149H19V46.5149Z"
				stroke="#0B5FFF"
				strokeMiterlimit="10"
				strokeWidth="2"
			/>
		</g>
		<path
			d="M19.0001 35.6968C24.0608 35.6968 28.1634 31.5943 28.1634 26.5336C28.1634 21.4729 24.0608 17.3704 19.0001 17.3704C13.9394 17.3704 9.83691 21.4729 9.83691 26.5336C9.83691 31.5943 13.9394 35.6968 19.0001 35.6968Z"
			fill="#0B5FFF"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeMiterlimit="10"
			strokeWidth="2"
		/>
		<path
			d="M15.2065 26.5338L17.7394 29.0666L22.7938 24.001"
			stroke="#F1F5FB"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
	</svg>
);

export {SecurityReportsIcon};
