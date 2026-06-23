/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayForm from '@clayui/form';
import BaseWarning from '~/components/BaseWarning/BaseWarning';
import BaseWrapper from '~/components/BaseWrapper/BaseWrapper';
import Checkbox from '~/components/Checkbox/Checkbox';
import DateRange from '~/components/DateRange/DateRange';
import Input from '~/components/FormFieldInput/FormFieldInput';
import MultiSelect from '~/components/FormMultiSelect/FormMultiSelect';
import Renderer from '~/components/FormRenderer/FormRenderer';
import Select from '~/components/FormSelect/FormSelect';

const Form = () => {};

Form.BaseWarning = BaseWarning;
Form.BaseWrapper = BaseWrapper;
Form.Clay = ClayForm;
Form.Checkbox = Checkbox;
Form.DateRange = DateRange;
Form.Divider = (props: React.HTMLAttributes<HTMLHRElement>) => (
	<hr {...props} />
);
Form.Input = Input;
Form.MultiSelect = MultiSelect;
Form.Select = Select;
Form.Renderer = Renderer;

export default Form;
