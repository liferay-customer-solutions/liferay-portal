/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {addDays} from 'date-fns';
import useSWR from 'swr';
import {
	METRIC_PARAMETER,
	MetricPeriod,
} from '~/pages/Admin/MPSummary/utils/constants';
import GraphQL from '~/services/headless/GraphQL';
import SearchBuilder from '~/utils/SearchBuilder';
import {getLastDayOfMonth} from '~/utils/dateUtils';
import {OrderWorkflowStatusCode} from '~/utils/orderUtils';

import type {Order} from '~/types/orders';

const useOrderMetrics = (param: MetricPeriod) => {
	return useSWR(['metrics/order', param], async () => {
		const currentTime = new Date();

		const beforeLastPeriod = addDays(
			currentTime,
			-METRIC_PARAMETER[param] * 2
		);

		const lastPeriod = addDays(currentTime, -METRIC_PARAMETER[param]);

		beforeLastPeriod.setHours(0, 0, 0);
		lastPeriod.setHours(23, 59, 59);

		const [
			{
				orders,
				ordersCreateBetweenLastPeriod,
				ordersCreatedLastPeriod,
				...metrics
			},
			ordersWithTotalAmount,
		] = await Promise.all([
			GraphQL.metrics(
				{
					group: 'headlessCommerceAdminOrder_v1_0',
					name: 'orders',
				},
				{
					orders: '',
					ordersCreateBetweenLastPeriod: new SearchBuilder()
						.lt('createDate', lastPeriod.toISOString())
						.and()
						.gt('createDate', beforeLastPeriod.toISOString())
						.build(),
					ordersCreatedLastPeriod: SearchBuilder.gt(
						'createDate',
						lastPeriod.toISOString()
					),
					ordersThisMonth: new SearchBuilder()
						.gt(
							'createDate',
							new Date(
								currentTime.getFullYear(),
								currentTime.getMonth(),
								1,
								0,
								0,
								0
							).toISOString()
						)
						.and()
						.lt(
							'createDate',
							new Date(
								currentTime.getFullYear(),
								currentTime.getMonth(),
								getLastDayOfMonth(
									currentTime.getMonth(),
									currentTime.getFullYear()
								),
								23,
								59,
								59
							).toISOString()
						)
						.build(),
					ordersThisYear: SearchBuilder.gt(
						'createDate',
						new Date(
							currentTime.getFullYear(),
							0,
							1,
							0,
							0,
							0
						).toISOString()
					),
				}
			).then(({data: {metrics}}) => ({
				orders: metrics.orders.totalCount,
				ordersCreateBetweenLastPeriod:
					metrics.ordersCreateBetweenLastPeriod.totalCount,
				ordersCreatedLastPeriod:
					metrics.ordersCreatedLastPeriod.totalCount,
				ordersThisMonth: metrics.ordersThisMonth.totalCount,
				ordersThisYear: metrics.ordersThisYear.totalCount,
			})),
			GraphQL.metrics(
				{
					group: 'headlessCommerceAdminOrder_v1_0',
					name: 'orders',
					options: {
						body: `items {totalAmount}`,
						pageSize: '-1',
					},
				},
				{
					totalAmount: new SearchBuilder()
						.gt('totalAmount', 0)
						.and()
						.lambda(
							'orderStatus',
							OrderWorkflowStatusCode.COMPLETED,
							{
								unquote: true,
							}
						)
						.build(),
				}
			),
		]);

		const newOrders =
			ordersCreatedLastPeriod - ordersCreateBetweenLastPeriod;

		let growth = Number(
			((newOrders / ordersCreatedLastPeriod) * 100).toFixed(2)
		);

		if (Number.isNaN(growth)) {
			growth = 0;
		}

		return {
			beforeLastPeriod: ordersCreateBetweenLastPeriod,
			growth,
			lastPeriod: ordersCreatedLastPeriod,
			totalAmount: (
				ordersWithTotalAmount.data.metrics.totalAmount.items as Order[]
			).reduce(
				(previousItem, currentItem) =>
					previousItem + (currentItem.totalAmount ?? 0),
				0
			),
			totalCount: orders,
			...metrics,
		};
	});
};

export default useOrderMetrics;
