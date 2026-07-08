/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderItemConstants;
import com.liferay.one.model.OrderItem;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class ProvisioningOrderService {

	public void cancelRealignedOrder(
			Order parentOrder,
			List<OpportunityLineItem> realignmentOpportunityLineItems,
			List<String> warningMessages)
		throws Exception {

		com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem[]
			parentOrderItems = parentOrder.getOrderItems();

		if (parentOrderItems == null) {
			return;
		}

		for (OpportunityLineItem realignmentOpportunityLineItem :
				realignmentOpportunityLineItems) {

			boolean matched = false;

			for (com.liferay.headless.commerce.admin.order.client.dto.v1_0.
					OrderItem parentOrderItem : parentOrderItems) {

				if (!Objects.equals(
						parentOrderItem.getSkuExternalReferenceCode(),
						realignmentOpportunityLineItem.getProduct2Id())) {

					continue;
				}

				matched = true;

				OrderItem orderItem =
					_commerceOrderItemService.fetchCommerceOrderItem(
						parentOrderItem.getId());

				if ((orderItem == null) ||
					!Objects.equals(
						orderItem.getStatus(),
						CommerceOrderItemConstants.STATUS_APPROVED)) {

					continue;
				}

				String endDate = _toDateTime(
					realignmentOpportunityLineItem.getEndDate());
				String startDate = _toDateTime(
					realignmentOpportunityLineItem.getServiceDate());

				if (Objects.equals(orderItem.getEndDate(), endDate) &&
					Objects.equals(orderItem.getStartDate(), startDate)) {

					continue;
				}

				_commerceOrderItemService.patchOrderItemCustomFields(
					orderItem.getCommerceOrderItemId(),
					Map.of(
						"customStatus",
						CommerceOrderItemConstants.STATUS_CANCELED));

				_entitlementService.deleteEntitlements(
					orderItem.getCommerceOrderItemId());

				if (Validator.isNotNull(endDate) &&
					!Objects.equals(orderItem.getEndDate(), endDate)) {

					_addWarning(
						warningMessages,
						StringBundler.concat(
							"End date mismatch for order item ",
							parentOrderItem.getExternalReferenceCode(),
							". Amended date: ", endDate, ", original date: ",
							orderItem.getEndDate()));
				}
			}

			if (!matched) {
				_addWarning(
					warningMessages,
					"Unable to find an order item for amended line " +
						realignmentOpportunityLineItem.getProductName());
			}
		}

		Order canceledOrder =
			_commerceOrderService.fetchOrderByExternalReferenceCode(
				parentOrder.getExternalReferenceCode());

		if ((canceledOrder == null) ||
			(canceledOrder.getOrderItems() == null)) {

			return;
		}

		for (com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem
				parentOrderItem : canceledOrder.getOrderItems()) {

			OrderItem orderItem =
				_commerceOrderItemService.fetchCommerceOrderItem(
					parentOrderItem.getId());

			if ((orderItem != null) &&
				!Objects.equals(
					orderItem.getStatus(),
					CommerceOrderItemConstants.STATUS_CANCELED)) {

				return;
			}
		}

		_commerceOrderService.cancelOrder(canceledOrder.getId());
	}

	public void trimRenewedOrderItems(
			long accountId, String opportunityId,
			List<OpportunityLineItem> opportunityLineItems,
			List<String> warningMessages)
		throws Exception {

		List<Order> orders = _commerceOrderService.getAccountOrders(accountId);

		for (Order order : orders) {
			if (Objects.equals(
					order.getExternalReferenceCode(), opportunityId) ||
				(order.getOrderItems() == null)) {

				continue;
			}

			for (com.liferay.headless.commerce.admin.order.client.dto.v1_0.
					OrderItem orderItem : order.getOrderItems()) {

				for (OpportunityLineItem opportunityLineItem :
						opportunityLineItems) {

					if (!Objects.equals(
							orderItem.getSkuExternalReferenceCode(),
							opportunityLineItem.getProduct2Id())) {

						continue;
					}

					String renewalStartDate = _toDateTime(
						opportunityLineItem.getServiceDate());

					if (Validator.isNull(renewalStartDate)) {
						continue;
					}

					OrderItem commerceOrderItem =
						_commerceOrderItemService.fetchCommerceOrderItem(
							orderItem.getId());

					if (commerceOrderItem == null) {
						continue;
					}

					String endDate = commerceOrderItem.getEndDate();

					if (!Objects.equals(
							commerceOrderItem.getStatus(),
							CommerceOrderItemConstants.STATUS_APPROVED) ||
						Validator.isNull(endDate)) {

						continue;
					}

					if (renewalStartDate.compareTo(endDate) < 0) {
						_addWarning(
							warningMessages,
							StringBundler.concat(
								"The renewal start date ", renewalStartDate,
								" is before the end date of order item ",
								orderItem.getExternalReferenceCode()));

						continue;
					}

					String effectiveEndDate =
						commerceOrderItem.getEffectiveEndDate();

					if (Validator.isNotNull(effectiveEndDate) &&
						(renewalStartDate.compareTo(effectiveEndDate) < 0)) {

						_commerceOrderItemService.patchOrderItemCustomFields(
							commerceOrderItem.getCommerceOrderItemId(),
							Map.of("effectiveEndDate", renewalStartDate));
					}
				}
			}
		}
	}

	private void _addWarning(
		List<String> warningMessages, String warningMessage) {

		warningMessages.add(warningMessage);

		if (_log.isWarnEnabled()) {
			_log.warn(warningMessage);
		}
	}

	private String _toDateTime(String value) {
		if (Validator.isNull(value)) {
			return null;
		}

		Matcher matcher = _datePattern.matcher(value);

		if (matcher.matches()) {
			return value + "T00:00:00Z";
		}

		return value;
	}

	private static final Log _log = LogFactory.getLog(
		ProvisioningOrderService.class);

	private static final Pattern _datePattern = Pattern.compile(
		"\\d{4}-\\d{2}-\\d{2}");

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementService _entitlementService;

}