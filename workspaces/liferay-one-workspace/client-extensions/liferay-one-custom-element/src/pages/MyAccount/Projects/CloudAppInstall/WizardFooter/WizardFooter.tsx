/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ComponentProps} from 'react';
import i18n from '~/i18n';

type WizardFooterProps = {
	backButtonProps?: ComponentProps<typeof ClayButton>;
	cancelButtonProps?: ComponentProps<typeof ClayButton>;
	continueButtonProps?: ComponentProps<typeof ClayButton>;
};

const WizardFooter = ({
	backButtonProps,
	cancelButtonProps,
	continueButtonProps,
}: WizardFooterProps) => (
	<div className="align-items-center d-flex justify-content-between mt-6 w-100">
		<div>
			{cancelButtonProps && (
				<ClayButton
					className="font-weight-semi-bold"
					displayType="unstyled"
					{...cancelButtonProps}
				>
					{cancelButtonProps?.children ?? i18n.translate('cancel')}
				</ClayButton>
			)}
		</div>

		<div>
			{backButtonProps && (
				<ClayButton displayType="secondary" {...backButtonProps}>
					{backButtonProps?.children ?? i18n.translate('back')}
				</ClayButton>
			)}

			{continueButtonProps && (
				<ClayButton className="ml-4" {...continueButtonProps}>
					{continueButtonProps?.children ??
						i18n.translate('continue')}
				</ClayButton>
			)}
		</div>
	</div>
);

export default WizardFooter;
