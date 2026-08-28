/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.permission.CommerceOrderPermission;
import com.liferay.one.service.CloudAppService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.ConsoleService;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Keven Leone
 * @author Ricardo Mariz
 */
@RequestMapping("/console")
@RestController
public class ConsoleRestController extends OneBaseRestController {

	@GetMapping("projects-usage")
	public ResponseEntity<String> getProjectsUsage(
			@AuthenticationPrincipal Jwt jwt)
		throws Exception {

		UserAccount userAccount = getMyUserAccount(jwt);

		return ResponseEntity.ok(
		).contentType(
			MediaType.APPLICATION_JSON
		).body(
			_consoleService.getProjectsUsage(userAccount.getEmailAddress())
		);
	}

	@PostMapping("provisioning/{orderId}")
	public ResponseEntity<Void> postProvisioningOrder(
			@AuthenticationPrincipal Jwt jwt, @PathVariable long orderId,
			@RequestBody String json)
		throws Exception {

		if (_log.isInfoEnabled()) {
			_log.info("Provisioning order " + orderId);
		}

		UserAccount userAccount = getMyUserAccount(jwt);

		_commerceOrderPermission.check(orderId, userAccount);

		Order order = _getCloudAppOrder(orderId);

		Integer paymentStatus = order.getPaymentStatus();

		if (!Objects.equals(
				paymentStatus,
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_COMPLETED) &&
			!Objects.equals(
				paymentStatus,
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED)) {

			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Skipping provisioning for order ", orderId,
						" with payment status ", paymentStatus));
			}

			return ResponseEntity.status(
				HttpStatus.CONFLICT
			).build();
		}

		JSONObject jsonObject = new JSONObject(json);

		String projectId = jsonObject.getString("projectId");

		_checkConsoleProject(projectId, userAccount);

		_cloudAppService.deployCloudApp(
			orderId, jsonObject.getLong("orderItemId"), projectId);

		return ResponseEntity.ok(
		).build();
	}

	@PostMapping("uninstall-app/{orderId}")
	public ResponseEntity<Void> postUninstallApp(
			@AuthenticationPrincipal Jwt jwt, @PathVariable long orderId,
			@RequestBody String json)
		throws Exception {

		_commerceOrderPermission.check(orderId, jwt);

		_getCloudAppOrder(orderId);

		JSONObject jsonObject = new JSONObject(json);

		_cloudAppService.uninstallCloudApp(
			jsonObject.getString("id"), orderId,
			jsonObject.getLong("orderItemId"));

		if (_log.isInfoEnabled()) {
			_log.info("Uninstalled app for order " + orderId);
		}

		return ResponseEntity.ok(
		).build();
	}

	private void _checkConsoleProject(String projectId, UserAccount userAccount)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			_consoleService.getProjectsUsage(userAccount.getEmailAddress()));

		JSONArray userProjectsJSONArray = jsonObject.getJSONArray(
			"userProjects");

		for (int i = 0; i < userProjectsJSONArray.length(); i++) {
			JSONObject userProjectJSONObject =
				userProjectsJSONArray.getJSONObject(i);

			JSONArray environmentsJSONArray =
				userProjectJSONObject.getJSONArray("environments");

			for (int j = 0; j < environmentsJSONArray.length(); j++) {
				JSONObject environmentJSONObject =
					environmentsJSONArray.getJSONObject(j);

				if (Objects.equals(
						environmentJSONObject.getString("projectId"),
						projectId)) {

					return;
				}
			}
		}

		throw new PrincipalException();
	}

	private Order _getCloudAppOrder(long orderId) throws Exception {
		Order order = _commerceOrderService.fetchCommerceOrder(orderId);

		if (order == null) {
			throw new IllegalArgumentException(
				"No order exists with ID " + orderId);
		}

		if (!Objects.equals(
				order.getOrderTypeExternalReferenceCode(), "CLOUD_APP")) {

			throw new IllegalArgumentException(
				"Unsupported order type: " +
					order.getOrderTypeExternalReferenceCode());
		}

		return order;
	}

	private static final Log _log = LogFactory.getLog(
		ConsoleRestController.class);

	@Autowired
	private CloudAppService _cloudAppService;

	@Autowired
	private CommerceOrderPermission _commerceOrderPermission;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private ConsoleService _consoleService;

}