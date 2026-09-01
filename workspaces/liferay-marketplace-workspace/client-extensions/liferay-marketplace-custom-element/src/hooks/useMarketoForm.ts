/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useRef, useState} from 'react';

import {waitTimeout} from '../utils/util';

type MktoForms2 = {
	loadForm: (
		baseURL: string,
		munchkinId: string,
		formId: string,
		callback: (form: any) => void
	) => void;
	whenReady: (fn: (form: any) => void) => void;
	whenRendered: (fn: (form: any) => void) => void;
};

declare global {
	interface Window {
		MktoForms2?: MktoForms2;
		mktoForms2BaseStyle?: HTMLLinkElement;
		mktoForms2ThemeStyle?: HTMLLinkElement;
	}
}

export type useMarketoProps = {
	footerElement?: (element: any) => void;
	formId: string;
	onSubmit?: () => void;
	submitText: string;
};

const defaultMktoForms2 = window.MktoForms2;

const baseURL = `//pages.liferay.com`;
const MARKETO_SUBMIT_TIMEOUT = 8000;
const MUNCHKIN_ID = '212-DQY-814';

const useMarketo = ({
	footerElement,
	formId,
	onSubmit,
	submitText,
}: useMarketoProps) => {
	const [form, setForm] = useState<any>();
	const [started, setStarted] = useState(false);
	const [formLoaded, setFormLoaded] = useState(false);
	const [MktoForms2, setMktoForms2] = useState(defaultMktoForms2);
	const submitResolveRef = useRef<(submitted: boolean) => void>();

	function triggerSubmit(values: unknown): Promise<boolean> {
		if (!form || !started) {
			console.error('Marketo form is not available');

			return Promise.resolve(false);
		}

		const submitted = new Promise<boolean>((resolve) => {
			submitResolveRef.current = resolve;
		});

		form.vals(values);

		form.submit();

		// The submission is only confirmed by the onSuccess callback below. The
		// timeout keeps the caller from waiting forever when Marketo never
		// answers, so the purchase always moves on.

		return Promise.race([
			submitted,
			waitTimeout(MARKETO_SUBMIT_TIMEOUT).then(() => {
				if (submitResolveRef.current) {
					submitResolveRef.current = undefined;

					console.error(
						'Marketo form submission was not confirmed',
						formId
					);
				}

				return false;
			}),
		]);
	}

	useEffect(() => {
		if (!MktoForms2) {
			const script = document.createElement('script');
			script.defer = true;
			script.onload = () => setMktoForms2(window.MktoForms2);
			script.src = `${baseURL}/js/forms2/js/forms2.min.js`;
			document.head.appendChild(script);

			return;
		}

		if (!formLoaded) {
			MktoForms2.loadForm(baseURL, MUNCHKIN_ID, formId, (form: any) => {
				setForm(form);

				const arrayify = getSelection.call.bind([].slice) as any;
				const formEl = form.getFormElem()[0];

				const styledElements = arrayify(
					formEl.querySelectorAll('[style]')
				).concat(formEl);

				formEl
					.querySelectorAll('style')
					.forEach((element: any) => element.remove());

				styledElements.forEach((element: any) =>
					element.removeAttribute('style')
				);

				const mktoForms2BaseStyle = window.mktoForms2BaseStyle;
				const mktoForms2ThemeStyle = window.mktoForms2ThemeStyle;
				const styleSheets = arrayify(document.styleSheets);

				styleSheets.forEach((stylesheet: StyleSheet) => {
					if (
						[mktoForms2BaseStyle, mktoForms2ThemeStyle].indexOf(
							(stylesheet as any).ownerNode
						) !== -1 ||
						formEl.contains(stylesheet.ownerNode)
					) {
						stylesheet.disabled = true;
					}
				});

				if (footerElement) {
					const buttonElem = form
						.getFormElem()
						.find('button.mktoButton');

					buttonElem.html(submitText);

					footerElement(buttonElem[0]);
				}

				form.onSuccess(() => {

					// eslint-disable-next-line no-console
					console.info('Submitting Marketo form', formId);

					submitResolveRef.current?.(true);

					submitResolveRef.current = undefined;

					onSubmit?.();

					return false;
				});

				setStarted(true);
			});

			setFormLoaded(true);
		}
	}, [MktoForms2, footerElement, formId, formLoaded, onSubmit, submitText]);

	return {
		MktoForms2,
		form,
		formLoaded,
		started,
		triggerSubmit,
	};
};

export default useMarketo;
