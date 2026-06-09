/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {sub, translate} from '../../../i18n';
import getKebabCase from '../../../utils/getKebabCase';
import BadgeButton from '../../BadgeButton/BadgeButton';
import Button from '../../Button/Button';
import {IFilterOption} from '../Filter';

export interface IProps {
	onChange: (selectedFilters: IFilterOption[]) => void;
	searchResultsCount: number;
	searchTerm: string;
	selectedFilters: IFilterOption[];
}

const FilterResults = ({
	onChange,
	searchResultsCount,
	searchTerm,
	selectedFilters,
}: IProps) => {
	const hasFilterValue = (selectedFilters: IFilterOption[]) => {
		return selectedFilters.some((option) => !!option.values.length);
	};

	const handleClearAllFilters = () => {
		onChange([]);
	};

	const handleClearFilter = (filterKey: string) => {
		onChange(
			selectedFilters.map((option) => {
				if (option.key === filterKey) {
					return {...option, values: []};
				}

				return option;
			})
		);
	};

	return (
		<>
			<div className="d-flex">
				{searchTerm !== '' && (
					<p className="font-weight-semi-bold m-0 mt-3 text-paragraph-sm">
						{searchResultsCount > 1
							? `${sub('x-results-for-x' as any, [
									searchResultsCount.toString(),
									`"${searchTerm}"`,
								])}`
							: `${sub('x-result-for-x' as any, [
									searchResultsCount.toString(),
									`"${searchTerm}"`,
								])}`}
					</p>
				)}
			</div>
			<div className="bd-highlight d-flex">
				<div className="bd-highlight col d-flex flex-wrap pl-0 pt-2 w-100">
					{selectedFilters.map((option) => {
						if (option.values.length) {
							return (
								<BadgeButton
									filterName={translate(option.name as any)}
									filterValue={option.values
										.map((value) =>
											translate(
												getKebabCase(value.key) as any
											)
										)
										.join(', ')}
									key={option.key}
									onClick={() =>
										handleClearFilter(option.key)
									}
								/>
							);
						}

						return null;
					})}
				</div>

				<div className="bd-highlight flex-shrink-2 pt-2">
					{hasFilterValue(selectedFilters) && (
						<Button
							borderless
							className="link"
							onClick={handleClearAllFilters}
							prependIcon="times-circle"
							small
						>
							{translate('clear-all-filters')}
						</Button>
					)}
				</div>
			</div>
		</>
	);
};

export default FilterResults;
