/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ClayDropDownWithItems} from '@clayui/drop-down';
import ClayIcon from '@clayui/icon';
import {useMemo} from 'react';
import {useParams, useSearchParams} from 'react-router-dom';
import BackLink from '~/components/BackLink/BackLink';
import {PageRenderer} from '~/components/Page/Page';
import useGetProductByOrderId from '~/hooks/useGetProductByOrderId';
import i18n from '~/i18n';
import DeliveryOrderModel from '~/models/DeliveryOrderModel';
import {DeliveryProductModel} from '~/models/DeliveryProductModel';
import useSSAActions from '~/pages/Admin/SSADashboard/hooks/useSSAActions';
import {safeJSONParse} from '~/utils/safeJSONParse';

import OrderDetailsHeader from './OrderDetailsHeader/OrderDetailsHeader';
import TrialDetailsBody from './TrialDetailsBody';

import type {PlacedOrder} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

type TrialActionsProps = {
	mutatePlacedOrder: ReturnType<typeof useGetProductByOrderId>['mutate'];
	placedOrder: PlacedOrder;
};

function TrialActions({mutatePlacedOrder, placedOrder}: TrialActionsProps) {
	const actions = useSSAActions();

	return (
		<ClayDropDownWithItems
			className="align-items-center cursor-pointer d-flex h-100"
			items={
				actions
					.filter((_, index) => index > 0)
					.map((action) => {
						const disabled =
							typeof action.disabled === 'function'
								? action.disabled(placedOrder)
								: action.disabled;

						const hidden =
							typeof action.hidden === 'function'
								? action.hidden(placedOrder)
								: action.hidden;

						return {
							...action,
							disabled,
							hidden,
							label: action.name,
							onClick: () =>
								action?.onClick?.(
									placedOrder,
									mutatePlacedOrder
								),
						};
					}) as React.ComponentProps<
					typeof ClayDropDownWithItems
				>['items']
			}
			trigger={
				<ClayButton displayType="secondary">
					{i18n.translate('manage-trial')}

					<ClayIcon className="ml-2" symbol="angle-down-small" />
				</ClayButton>
			}
		/>
	);
}

export default function TrialDetails() {
	const {orderId} = useParams();
	const {
		data,
		error,
		isLoading,
		mutate: mutatePlacedOrder,
	} = useGetProductByOrderId(orderId as string);

	const product = data?.product as DeliveryProduct;
	const placedOrder = data?.placedOrder as PlacedOrder;

	const [params] = useSearchParams();
	const parentPath = params.get('from') ?? '/';

	const productModel = useMemo(
		() => new DeliveryProductModel(product || {}),
		[product]
	);

	const orderModel = useMemo(
		() => new DeliveryOrderModel(placedOrder),
		[placedOrder]
	);

	const {projectId} = safeJSONParse(orderModel.customFields.TRIAL_SETTINGS, {
		projectId: orderId,
	});

	return (
		<PageRenderer
			className="app-details-header d-flex flex-column w-100"
			error={error}
			isLoading={isLoading || !placedOrder || !productModel}
		>
			<BackLink path={parentPath}>
				{i18n.translate('back-to-the-list')}
			</BackLink>

			<div className="d-flex justify-content-between">
				<OrderDetailsHeader
					className="d-flex flex-row justify-content-between pb-3 pt-5"
					hasOrderDetails
					image={orderModel.productThumbnail}
					name={projectId}
					productOwner={productModel?.catalogName}
				/>

				<TrialActions
					mutatePlacedOrder={mutatePlacedOrder}
					placedOrder={placedOrder}
				/>
			</div>

			<TrialDetailsBody
				orderModel={orderModel}
				placedOrder={placedOrder}
				productModel={productModel}
				projectId={projectId as string}
			/>
		</PageRenderer>
	);
}
