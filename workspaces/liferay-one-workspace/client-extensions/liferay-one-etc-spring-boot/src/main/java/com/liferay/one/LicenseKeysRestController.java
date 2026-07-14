/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.admin.user.client.problem.Problem;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.ClassNameConstants;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.model.LicenseKey;
import com.liferay.one.model.SubscriptionEntry;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.LicenseKeyService;
import com.liferay.one.service.SubscriptionEntryService;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.util.GetterUtil;

import java.util.Objects;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Amos Fong
 */
@RequestMapping("/license-keys")
@RestController
public class LicenseKeysRestController extends OneBaseRestController {

	@DeleteMapping("/subscriptions")
	public void deleteSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		for (long licenseKeyId : licenseKeyIds) {
			_subscriptionEntryService.deleteSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());
		}
	}

	@GetMapping("/subscriptions")
	public boolean getSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyId") long licenseKeyId)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		SubscriptionEntry subscriptionEntry =
			_subscriptionEntryService.fetchSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());

		if (subscriptionEntry != null) {
			return true;
		}

		return false;
	}

	@PostMapping("/type-free")
	public LicenseKey postLicenseKeysTypeFree(@RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		String domains = jsonObject.optString("domains");
		String orderId = jsonObject.optString("orderId");
		String owner = jsonObject.optString("owner");

		if (_licenseKeyService.hasLicenseKeyTypeFree(domains, owner)) {
			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"A license key was already provisioned for the owner with " +
					"this domain");
		}

		Order order = _commerceOrderService.getCommerceOrder(
			GetterUtil.getLong(orderId));

		Account account = order.getAccount();

		LicenseKey licenseKey = _licenseKeyService.addLicenseKeyTypeFree(
			account.getId(), domains, orderId, owner);

		Integer orderStatus = order.getOrderStatus();

		if ((orderStatus == null) ||
			(orderStatus != CommerceOrderConstants.ORDER_STATUS_COMPLETED)) {

			_commerceOrderService.completeOrder(
				order.getId(),
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED);
		}

		return licenseKey;
	}

	@PostMapping("/type-free-domains-check")
	public void postLicenseKeysTypeFreeDomainsCheck(@RequestBody String json)
		throws Exception {

		JSONObject jsonObject = new JSONObject(json);

		if (_licenseKeyService.hasLicenseKeyTypeFree(
				jsonObject.optString("domains"),
				jsonObject.optString("owner"))) {

			throw new ResponseStatusException(
				HttpStatus.CONFLICT,
				"A license key was already provisioned for the owner with " +
					"this domain");
		}
	}

	@PutMapping("/subscriptions")
	public void putSubscriptions(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam("licenseKeyIds") long[] licenseKeyIds)
		throws Exception {

		for (long licenseKeyId : licenseKeyIds) {
			LicenseKey licenseKey = _licenseKeyService.getLicenseKey(
				jwt, licenseKeyId);

			_checkAccountViewPermission(licenseKey.getAccountEntryId(), jwt);
		}

		UserAccount userAccount = getMyUserAccount(jwt);

		for (long licenseKeyId : licenseKeyIds) {
			_subscriptionEntryService.addSubscriptionEntry(
				jwt, ClassNameConstants.LICENSE_KEY, licenseKeyId,
				userAccount.getId());
		}
	}

	private void _checkAccountViewPermission(long accountEntryId, Jwt jwt)
		throws Exception {

		try {
			_accountService.getAccount(accountEntryId, jwt);
		}
		catch (Problem.ProblemException problemException) {
			Problem problem = problemException.getProblem();

			if ((problem != null) &&
				(Objects.equals(
					HttpStatus.FORBIDDEN.name(), problem.getStatus()) ||
				 Objects.equals(
					 HttpStatus.NOT_FOUND.name(), problem.getStatus()))) {

				throw new PrincipalException();
			}

			throw problemException;
		}
	}

	@Autowired
	private AccountService _accountService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private LicenseKeyService _licenseKeyService;

	@Autowired
	private SubscriptionEntryService _subscriptionEntryService;

}