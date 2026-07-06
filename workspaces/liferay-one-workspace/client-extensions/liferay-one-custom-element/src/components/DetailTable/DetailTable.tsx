/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import React, {HTMLAttributes, ReactNode} from 'react';

import './DetailTable.css';

export enum Orientation {
	HORIZONTAL = 'HORIZONTAL',
	VERTICAL = 'VERTICAL',
}

type DetailTableItem = {
	className?: HTMLAttributes<HTMLTableRowElement>['className'];
	divider?: boolean;
	flexHeading?: boolean;
	title: string | ReactNode;
	value: string | ReactNode;
	visible?: boolean;
};

type DetailTableProps = {
	columns?: number;
	items: DetailTableItem[];
	orientation?: keyof typeof Orientation;
};

const DetailTable: React.FC<DetailTableProps> = ({
	columns = 1,
	items,
	orientation = Orientation.HORIZONTAL,
}) => (
	<table className="detail-table w-100">
		<tbody
			className={classNames({
				'd-flex flex-wrap': columns > 1,
			})}
		>
			{items
				.filter(({visible = true}) => visible)
				.map((item, index) => (
					<React.Fragment key={index}>
						<tr
							className={classNames(item.className, {
								'd-flex flex-column':
									orientation === Orientation.VERTICAL,
							})}
							key={index}
							style={{
								width:
									columns > 1
										? `${100 / columns}%`
										: undefined,
							}}
						>
							<th
								className={classNames(
									'tr-detail-table__header',
									{
										'd-flex': item.flexHeading,
									}
								)}
							>
								{item.title}
							</th>

							<td>{item.value}</td>
						</tr>

						{item.divider && (
							<tr>
								<td>
									<hr />
								</td>

								<td>
									<hr />
								</td>
							</tr>
						)}
					</React.Fragment>
				))}
		</tbody>
	</table>
);

export default DetailTable;
