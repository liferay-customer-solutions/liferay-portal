/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {SVGProps} from 'react';
import {JSX} from 'react/jsx-runtime';

const SubmitATicketHoverIcon = (
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
			d="M36.5997 4L33.8197 28.91L29.5697 24.8L22.3897 37.23L14.9297 32.93L22.1097 20.49L16.4197 18.87L36.5997 4Z"
			fill="#0B5FFF"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
		<path
			d="M12.48 24.6201L10.33 28.3501L5 25.2801L10.13 16.4001L6.07 15.2301L20.48 4.62012L19.64 12.1801"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
		<path
			d="M38.2998 24.4901L43.2998 20.8101L41.3198 38.6101L38.2798 35.6701L33.1498 44.5501L27.8198 41.4701L32.9498 32.5901"
			stroke="#0B5FFF"
			strokeLinecap="round"
			strokeLinejoin="round"
			strokeWidth="2"
		/>
	</svg>
);

export {SubmitATicketHoverIcon};
