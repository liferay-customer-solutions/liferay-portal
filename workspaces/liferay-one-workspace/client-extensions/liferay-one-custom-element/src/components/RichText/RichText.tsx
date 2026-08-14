/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {KeyboardEvent, useState} from 'react';
import ReactQuill from 'react-quill';
import {removeHTMLTags} from '~/utils/stringUtils';

import './RichText.css';

type RichTextProps = {
	maxLength?: number;
	onChange: (value: string) => void;
	placeholder?: string;
	value: string;
};

const RELEASE_TIMEOUT = 1000;

const RichText = ({
	maxLength,
	onChange,
	placeholder,
	value = '',
}: RichTextProps) => {
	const [readOnly, setReadOnly] = useState(false);

	const length = removeHTMLTags(value).length;

	const onKeyDown = (event: KeyboardEvent) => {
		setReadOnly(false);

		if (event.key !== 'Backspace' && length === maxLength) {
			setReadOnly(true);
		}

		setTimeout(() => setReadOnly(false), RELEASE_TIMEOUT);
	};

	return (
		<div className="rich-text">
			<ReactQuill
				{...(maxLength && {onKeyDown})}
				className={classNames({'mb-1': maxLength})}
				onChange={onChange}
				placeholder={placeholder}
				readOnly={readOnly}
				value={value}
			/>

			{!!maxLength && (
				<small className="text-neutral-7">
					{`${length}/${maxLength}`}
				</small>
			)}
		</div>
	);
};

export default RichText;
