/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {FormHTMLAttributes, ReactNode} from 'react';
import {FieldValues, FormProvider, UseFormReturn} from 'react-hook-form';
import Checkbox from '~/components/Checkbox/Checkbox';
import FormControl from '~/pages/Admin/SSADashboard/components/MarketplaceFormControl/MarketplaceFormControl';
import {MarketplaceFormHelpMessage} from '~/pages/Admin/SSADashboard/components/MarketplaceFormHelpMessage/MarketplaceFormHelpMessage';
import {MarketplaceFormInput} from '~/pages/Admin/SSADashboard/components/MarketplaceFormInput/MarketplaceFormInput';
import {MarketplaceFormLabel} from '~/pages/Admin/SSADashboard/components/MarketplaceFormLabel/MarketplaceFormLabel';

function Divider(props: React.HTMLAttributes<HTMLHRElement>) {
	<hr {...props} />;
}

type FormProps = {
	children: ReactNode;
	formProviderProps: UseFormReturn<FieldValues>;
} & FormHTMLAttributes<HTMLFormElement>;

type FormChildrens = {
	Checkbox: typeof Checkbox;
	Divider: typeof Divider;
	FormControl: typeof FormControl;
	HelpMessage: typeof MarketplaceFormHelpMessage;
	Input: typeof MarketplaceFormInput;
	Label: typeof MarketplaceFormLabel;
};

const MarketplaceForm: React.FC<FormProps> & FormChildrens = ({
	children,
	formProviderProps,
	...formProps
}) => (
	<FormProvider {...formProviderProps}>
		<form className="my-3 space-y-5" {...formProps}>
			{children}
		</form>
	</FormProvider>
);

MarketplaceForm.Checkbox = Checkbox;
MarketplaceForm.Divider = Divider;
MarketplaceForm.FormControl = FormControl;
MarketplaceForm.HelpMessage = MarketplaceFormHelpMessage;
MarketplaceForm.Input = MarketplaceFormInput;
MarketplaceForm.Label = MarketplaceFormLabel;

export default MarketplaceForm;
