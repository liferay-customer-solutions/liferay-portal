/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.OrderItem;
import com.liferay.one.salesforce.model.SalesforceOpportunityLineItem;
import com.liferay.one.util.CommerceOrderItemUtil;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.GetterUtil;

import java.time.Instant;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class ProvisioningOrderService {

	public void trimRealignedOrderItems(
			long accountId, String opportunityId, String parentOpportunityId,
			List<SalesforceOpportunityLineItem>
				realignmentSalesforceOpportunityLineItems,
			List<String> warningMessages)
		throws Exception {

		List<Order> orders = _commerceOrderService.getAccountOrders(accountId);

		for (SalesforceOpportunityLineItem
				realignmentSalesforceOpportunityLineItem :
					realignmentSalesforceOpportunityLineItems) {

			boolean matched = false;

			for (Order order : orders) {
				if (Objects.equals(
						order.getExternalReferenceCode(), opportunityId) ||
					(order.getOrderItems() == null) ||
					!_isFamilyOrder(order, parentOpportunityId)) {

					continue;
				}

				for (OrderItem parentOrderItem : order.getOrderItems()) {
					if (!Objects.equals(
							parentOrderItem.getSkuExternalReferenceCode(),
							realignmentSalesforceOpportunityLineItem.
								getProduct2Id())) {

						continue;
					}

					matched = true;

					if (!CommerceOrderItemUtil.isApproved(parentOrderItem)) {
						continue;
					}

					Instant endDateInstant =
						realignmentSalesforceOpportunityLineItem.
							getEndDateInstant();
					Instant startDateInstant =
						realignmentSalesforceOpportunityLineItem.
							getServiceDateInstant();

					if (Objects.equals(
							CommerceOrderItemUtil.getEndDateInstant(
								parentOrderItem),
							endDateInstant) &&
						Objects.equals(
							CommerceOrderItemUtil.getStartDateInstant(
								parentOrderItem),
							startDateInstant)) {

						continue;
					}

					Instant effectiveEndDateInstant = endDateInstant;

					if (effectiveEndDateInstant == null) {
						effectiveEndDateInstant = startDateInstant;
					}

					if (effectiveEndDateInstant == null) {
						effectiveEndDateInstant = Instant.now();
					}

					Instant orderItemEffectiveEndDateInstant =
						CommerceOrderItemUtil.getEffectiveEndDateInstant(
							parentOrderItem);

					if ((orderItemEffectiveEndDateInstant == null) ||
						orderItemEffectiveEndDateInstant.isAfter(
							effectiveEndDateInstant)) {

						_commerceOrderItemService.patchOrderItemCustomFields(
							parentOrderItem.getId(),
							Map.of(
								"effectiveEndDate",
								effectiveEndDateInstant.toString()));

						if ((endDateInstant != null) &&
							!Objects.equals(
								CommerceOrderItemUtil.getEndDateInstant(
									parentOrderItem),
								endDateInstant)) {

							_addWarning(
								warningMessages,
								StringBundler.concat(
									"End date mismatch for order item ",
									parentOrderItem.getExternalReferenceCode(),
									". Amended date: ", endDateInstant,
									", original date: ",
									CommerceOrderItemUtil.getEndDateInstant(
										parentOrderItem)));
						}
					}

					_entitlementService.trimEntitlements(
						parentOrderItem.getId(),
						effectiveEndDateInstant.toString());
				}
			}

			if (!matched) {
				String productName =
					realignmentSalesforceOpportunityLineItem.getProductName();

				_addWarning(
					warningMessages,
					"Unable to find an order item for amended line " +
						productName);
			}
		}
	}

	public void trimRenewedOrderItems(
			long accountId, String opportunityId,
			List<SalesforceOpportunityLineItem> salesforceOpportunityLineItems,
			List<String> warningMessages)
		throws Exception {

		List<Order> orders = _commerceOrderService.getAccountOrders(accountId);

		for (Order order : orders) {
			if (Objects.equals(
					order.getExternalReferenceCode(), opportunityId) ||
				(order.getOrderItems() == null)) {

				continue;
			}

			for (OrderItem orderItem : order.getOrderItems()) {
				for (SalesforceOpportunityLineItem
						salesforceOpportunityLineItem :
							salesforceOpportunityLineItems) {

					if (!Objects.equals(
							orderItem.getSkuExternalReferenceCode(),
							salesforceOpportunityLineItem.getProduct2Id())) {

						continue;
					}

					Instant renewalStartDateInstant =
						salesforceOpportunityLineItem.getServiceDateInstant();

					if (renewalStartDateInstant == null) {
						continue;
					}

					Instant endDateInstant =
						CommerceOrderItemUtil.getEndDateInstant(orderItem);

					if (!CommerceOrderItemUtil.isApproved(orderItem) ||
						(endDateInstant == null)) {

						continue;
					}

					if (renewalStartDateInstant.isBefore(endDateInstant)) {
						_addWarning(
							warningMessages,
							StringBundler.concat(
								"The renewal start date ",
								renewalStartDateInstant,
								" is before the end date of order item ",
								orderItem.getExternalReferenceCode()));

						continue;
					}

					Instant effectiveEndDateInstant =
						CommerceOrderItemUtil.getEffectiveEndDateInstant(
							orderItem);

					if ((effectiveEndDateInstant != null) &&
						renewalStartDateInstant.isBefore(
							effectiveEndDateInstant)) {

						_commerceOrderItemService.patchOrderItemCustomFields(
							orderItem.getId(),
							Map.of(
								"effectiveEndDate",
								renewalStartDateInstant.toString()));
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

	private boolean _isFamilyOrder(Order order, String parentOpportunityId) {
		if (Objects.equals(
				order.getExternalReferenceCode(), parentOpportunityId)) {

			return true;
		}

		Map<String, Object> customFields =
			(Map<String, Object>)order.getCustomFields();

		if (customFields == null) {
			return false;
		}

		return Objects.equals(
			GetterUtil.getString(customFields.get("parentOpportunityId")),
			parentOpportunityId);
	}

	private static final Log _log = LogFactory.getLog(
		ProvisioningOrderService.class);

	@Autowired
	private CommerceOrderItemService _commerceOrderItemService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementService _entitlementService;

}