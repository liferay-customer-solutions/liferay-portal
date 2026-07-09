/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayButtonWithIcon} from '@clayui/button';
import {ClayTooltipProvider} from '@clayui/tooltip';

type PopoverIconProps = {
	symbol?: string;
	title: string;
};

export default function PopoverIcon({
	symbol = 'question-circle-full',
	title,
}: PopoverIconProps) {
	return (
		<ClayTooltipProvider>
			<ClayButtonWithIcon
				aria-label={title}
				data-tooltip-align="top"
				displayType={null}
				size="sm"
				symbol={symbol}
				title={title}
			/>
		</ClayTooltipProvider>
	);
}
