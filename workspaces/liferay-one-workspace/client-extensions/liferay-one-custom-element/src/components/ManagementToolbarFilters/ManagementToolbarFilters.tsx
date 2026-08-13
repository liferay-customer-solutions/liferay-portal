/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown, {Align} from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import classNames from 'classnames';
import {
	useCallback,
	useContext,
	useEffect,
	useMemo,
	useRef,
	useState,
} from 'react';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import useSWR from 'swr';
import Form from '~/components/Form/Form';
import {FieldOptions} from '~/components/FormRenderer/FormRenderer';
import {
	ListViewContext,
	ListViewTypes,
} from '~/components/ListView/context/ListViewContextProvider';
import useUpdateUrlParams from '~/components/ListView/hooks/useUpdateUrlParams';
import i18n from '~/i18n';
import fetcher from '~/services/fetcher/fetcher';
import {FilterSchema, RendererFields} from '~/types/filters';
import CreateFilters from '~/utils/CreateFilters';
import {safeJSONParse} from '~/utils/safeJSONParse';

import './ManagementToolbarFilters.css';

type ManagementToolbarFilterProps = {
	availableOptions?: FieldOptions;
	filterSchema?: FilterSchema;
};

export type Option = {label: string; value: string};

type FilterBodyProps = {
	availableOptions?: FieldOptions;
	filterSchema: FilterSchema | undefined;
	setIsVisible: React.Dispatch<React.SetStateAction<boolean>>;
};

const FilterBody: React.FC<FilterBodyProps> = ({
	availableOptions,
	filterSchema,
	setIsVisible,
}) => {
	const [listViewContext, dispatch] = useContext(ListViewContext);
	const location = useLocation();
	const navigate = useNavigate();
	const params = useParams();
	const updateUrlParams = useUpdateUrlParams();

	const fields = useMemo(
		() =>
			(filterSchema?.fields as RendererFields[])?.filter(
				({name}) =>
					!availableOptions?.[name] || availableOptions[name].length
			),
		[availableOptions, filterSchema?.fields]
	);

	const initialFilters = useMemo(() => {
		const initialValues: Record<string, unknown> = {};

		for (const field of fields) {
			initialValues[field.name] = '';
		}

		return initialValues;
	}, [fields]);

	const [activeFieldName, setActiveFieldName] = useState<string | null>(null);

	const [form, setForm] = useState(() => ({
		...initialFilters,
		...listViewContext.filters.filter,
	}));

	const clearButtonDisabled = Object.values(form).every(
		(value) => !value || !(value as string | unknown[]).length
	);

	const onChange = (event: {
		target: {
			name: string;
			options?: HTMLOptionsCollection;
			type?: string;
			value: unknown;
		};
	}) => {
		const {
			target: {name, options, type},
		} = event;

		let {value} = event.target;

		if (type === 'date-range') {
			value = [
				{
					label: value,
					value,
				},
			];
		}
		else if (type === 'select-one') {
			value = [
				{
					label: options?.item(options.selectedIndex)?.label ?? '',
					value: Number(value) || value,
				},
			];
		}

		if (Array.isArray(value)) {
			if (!value[0]) {
				value = '';
			}
			else if (typeof value[0] === 'object') {
				value = !value[0].label ? '' : value;
			}
		}

		setForm({
			...form,
			[name]: value,
		});
	};

	const handleRemoveItemFromFilter = useCallback(() => {
		const searchParams = new URLSearchParams(location.search);
		searchParams.delete('filter');
		searchParams.delete('filterSchema');

		return navigate({
			search: `?${searchParams.toString()}`,
		});
	}, [location.search, navigate]);

	const paramsMemoized = useMemo(() => {
		return JSON.stringify({...params});
	}, [params]);

	const fieldsMemoized = useMemo(() => filterSchema?.fields, [filterSchema]);

	const {data: resourceOptions = {}, isLoading} = useSWR(
		filterSchema?.fields?.length ? `/filter-${filterSchema?.name}` : null,
		async () => {
			const parameters = safeJSONParse(paramsMemoized, {});

			const fieldsWithResource = fieldsMemoized?.filter(
				({resource}) => resource
			);

			const _fieldOptions: Record<string, unknown[]> = {};

			if (fieldsWithResource) {
				await Promise.all(
					fieldsWithResource.map((field) =>
						fetcher(
							(typeof field.resource === 'function'
								? field.resource(parameters)
								: field.resource) as string
						)
					)
				).then((results) =>
					results.forEach((result, index) => {
						const field = fieldsWithResource[index];

						if (field.transformData) {
							const parsedValue = field.transformData(result);

							_fieldOptions[field.name] =
								parsedValue as unknown[];
						}
					})
				);
			}

			return _fieldOptions;
		}
	);

	const fieldOptions = useMemo(
		() => ({...resourceOptions, ...availableOptions}),
		[availableOptions, resourceOptions]
	);

	const applyFilters = useCallback(
		(appliedForm: Record<string, unknown>) => {
			const filterCleaned = CreateFilters.removeEmptyFilter(
				appliedForm as {
					[key: string]: string | number | string[] | number[];
				}
			);

			const entries = Object.keys(filterCleaned).map((key) => ({
				label: fields?.find(({name}) => name === key)?.label ?? key,
				name: key,
				value: filterCleaned[key] as string,
			}));

			const filters = Object.keys(filterCleaned).map((key) => ({
				name: key,
				value: Array.isArray(filterCleaned[key])
					? (filterCleaned as unknown as Record<string, Option[]>)[
							key
						].map((options: Option) =>
							options?.value
								? options?.value
								: options?.label || options
						)
					: filterCleaned[key],
			}));

			const formattedFilter = filters.reduce(
				(previousValue, currentValue) => {
					return {
						...previousValue,
						[currentValue.name]: currentValue.value,
					};
				},
				{}
			);

			if (filterSchema) {
				updateUrlParams({
					filter: JSON.stringify(formattedFilter),
					filterSchema: filterSchema?.name as string,
					page: '1',
				});
			}

			if (!Object.keys(formattedFilter).length) {
				handleRemoveItemFromFilter();
			}

			dispatch({
				payload: {filters: {entries, filter: filterCleaned}},
				type: ListViewTypes.SET_FILTERS,
			});

			setIsVisible(false);
		},
		[
			dispatch,
			fields,
			filterSchema,
			handleRemoveItemFromFilter,
			setIsVisible,
			updateUrlParams,
		]
	);

	const onApply = useCallback(() => applyFilters(form), [applyFilters, form]);

	const onClearAll = useCallback(() => {
		setForm(initialFilters);
		applyFilters(initialFilters);
	}, [applyFilters, initialFilters]);

	useEffect(() => {
		const searchParams = new URLSearchParams(location.search);

		if (!searchParams.get('filter')) {
			setForm(initialFilters);
		}
	}, [initialFilters, location.search]);

	const activeField = fields?.find(({name}) => name === activeFieldName);

	if (activeField) {
		return (
			<div className="management-toolbar-filter-panel">
				<button
					className="management-toolbar-filter-back"
					onClick={() => setActiveFieldName(null)}
					type="button"
				>
					<ClayIcon symbol="angle-left" />

					<span className="management-toolbar-filter-title">
						{activeField.label}
					</span>
				</button>

				<div className="management-toolbar-filter-options">
					<Form.Renderer
						fieldOptions={fieldOptions}
						fields={[{...activeField, label: ''}]}
						filterSchema={filterSchema?.name as string}
						form={form}
						isLoading={isLoading}
						onApply={onApply}
						onChange={onChange}
					/>
				</div>

				<ClayButton
					className="management-toolbar-filter-apply w-100"
					onClick={onApply}
				>
					{i18n.translate('add-filter')}
				</ClayButton>
			</div>
		);
	}

	return (
		<div className="management-toolbar-filter-panel">
			<div className="management-toolbar-filter-heading">
				{i18n.translate('filter-by')}
			</div>

			{fields?.map((field) => (
				<button
					className="align-items-center d-flex justify-content-between management-toolbar-filter-category"
					key={field.name}
					onClick={() => setActiveFieldName(field.name)}
					type="button"
				>
					{field.label}

					<ClayIcon symbol="angle-right" />
				</button>
			))}

			{!clearButtonDisabled && (
				<ClayButton
					className="management-toolbar-filter-apply w-100"
					displayType="secondary"
					onClick={onClearAll}
				>
					{i18n.translate('clear')}
				</ClayButton>
			)}
		</div>
	);
};

const ManagementToolbarFilters: React.FC<ManagementToolbarFilterProps> = ({
	availableOptions,
	filterSchema,
}) => {
	const buttonRef = useRef<HTMLButtonElement | null>(null);

	const [isVisible, setIsVisible] = useState(false);

	const hasOneFilter = filterSchema?.fields?.length === 1;

	const handleExpand = (
		event: React.MouseEvent<HTMLButtonElement, MouseEvent>
	) => {
		buttonRef.current = event.target as HTMLButtonElement;

		setIsVisible((isVisible) => !isVisible);
	};

	return (
		<>
			<div className="align-items-center d-flex justify-content-between">
				<ClayButton
					className="align-items-center btn-secondary d-flex justify-content-between management-toolbar-filter-button ml-3 mr-2 px-2"
					displayType="unstyled"
					onClick={handleExpand}
				>
					<ClayIcon className="mr-2" symbol="filter" />

					{i18n.translate('filter')}

					<ClayIcon className="ml-2" symbol="caret-bottom" />
				</ClayButton>
			</div>

			{isVisible && (
				<ClayDropDown.Menu
					active={isVisible}
					alignElementRef={buttonRef}
					alignmentPosition={Align.BottomLeft}
					className={classNames('management-toolbar-dropdown', {
						'dropdown-management-toolbar-small': hasOneFilter,
					})}
					closeOnClickOutside
					onActiveChange={() =>
						setIsVisible((isVisible) => !isVisible)
					}
				>
					<div className="management-toolbar-dropdown-body">
						<FilterBody
							availableOptions={availableOptions}
							filterSchema={filterSchema}
							setIsVisible={setIsVisible}
						/>
					</div>
				</ClayDropDown.Menu>
			)}
		</>
	);
};

export default ManagementToolbarFilters;
