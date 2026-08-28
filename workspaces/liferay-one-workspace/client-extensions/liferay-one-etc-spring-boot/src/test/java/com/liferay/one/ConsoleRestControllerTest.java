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
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Ricardo Mariz
 */
public class ConsoleRestControllerTest {

	@BeforeEach
	public void setUp() throws Exception {
		_consoleRestController = new ConsoleRestController();

		_cloudAppService = Mockito.mock(CloudAppService.class);
		_commerceOrderPermission = Mockito.mock(CommerceOrderPermission.class);
		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_consoleService = Mockito.mock(ConsoleService.class);
		_userAccountService = Mockito.mock(UserAccountService.class);

		UserAccount userAccount = new UserAccount();

		userAccount.setEmailAddress(_EMAIL_ADDRESS);

		Mockito.when(
			_userAccountService.getMyUserAccount(null)
		).thenReturn(
			userAccount
		);

		Mockito.when(
			_consoleService.getProjectsUsage(_EMAIL_ADDRESS)
		).thenReturn(
			_createProjectsUsageJSON(_PROJECT_ID)
		);

		ReflectionTestUtils.setField(
			_consoleRestController, "_cloudAppService", _cloudAppService);
		ReflectionTestUtils.setField(
			_consoleRestController, "_commerceOrderPermission",
			_commerceOrderPermission);
		ReflectionTestUtils.setField(
			_consoleRestController, "_commerceOrderService",
			_commerceOrderService);
		ReflectionTestUtils.setField(
			_consoleRestController, "_consoleService", _consoleService);
		ReflectionTestUtils.setField(
			_consoleRestController, "_userAccountService", _userAccountService);
	}

	@Test
	public void testGetProjectsUsage() throws Exception {
		ResponseEntity<String> responseEntity =
			_consoleRestController.getProjectsUsage(null);

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		Assertions.assertEquals(
			_createProjectsUsageJSON(_PROJECT_ID), responseEntity.getBody());
	}

	@Test
	public void testPostProvisioningOrderDeploysAuthorizedProject()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		ResponseEntity<Void> responseEntity =
			_consoleRestController.postProvisioningOrder(
				null, _ORDER_ID, _createProvisioningJSON(_PROJECT_ID));

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		Mockito.verify(
			_cloudAppService
		).deployCloudApp(
			_ORDER_ID, _ORDER_ITEM_ID, _PROJECT_ID
		);
	}

	@Test
	public void testPostProvisioningOrderRejectsForeignProject()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		Assertions.assertThrows(
			PrincipalException.class,
			() -> _consoleRestController.postProvisioningOrder(
				null, _ORDER_ID, _createProvisioningJSON("someone-else-prd")));

		_verifyNeverDeployed();
	}

	@Test
	public void testPostProvisioningOrderRejectsOtherOrderType()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder(
				"DXP_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		Assertions.assertThrows(
			IllegalArgumentException.class,
			() -> _consoleRestController.postProvisioningOrder(
				null, _ORDER_ID, _createProvisioningJSON(_PROJECT_ID)));

		_verifyNeverDeployed();
	}

	@Test
	public void testPostProvisioningOrderReturnsConflictForPendingPayment()
		throws Exception {

		_whenFetchCommerceOrder(
			_createOrder("CLOUD_APP", _PAYMENT_STATUS_PENDING));

		ResponseEntity<Void> responseEntity =
			_consoleRestController.postProvisioningOrder(
				null, _ORDER_ID, _createProvisioningJSON(_PROJECT_ID));

		Assertions.assertEquals(
			HttpStatus.CONFLICT, responseEntity.getStatusCode());

		_verifyNeverDeployed();
	}

	@Test
	public void testPostUninstallApp() throws Exception {
		_whenFetchCommerceOrder(
			_createOrder(
				"CLOUD_APP",
				CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED));

		ResponseEntity<Void> responseEntity =
			_consoleRestController.postUninstallApp(
				null, _ORDER_ID,
				new JSONObject(
				).put(
					"id", _DEPLOYMENT_ID
				).put(
					"orderItemId", _ORDER_ITEM_ID
				).toString());

		Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

		Mockito.verify(
			_cloudAppService
		).uninstallCloudApp(
			_DEPLOYMENT_ID, _ORDER_ID, _ORDER_ITEM_ID
		);
	}

	private Order _createOrder(
		String orderTypeExternalReferenceCode, int paymentStatus) {

		Order order = new Order();

		order.setOrderTypeExternalReferenceCode(orderTypeExternalReferenceCode);
		order.setPaymentStatus(paymentStatus);

		return order;
	}

	private String _createProjectsUsageJSON(String projectId) {
		return new JSONObject(
		).put(
			"userProjects",
			new JSONArray(
			).put(
				new JSONObject(
				).put(
					"environments",
					new JSONArray(
					).put(
						new JSONObject(
						).put(
							"projectId", projectId
						)
					)
				)
			)
		).toString();
	}

	private String _createProvisioningJSON(String projectId) {
		return new JSONObject(
		).put(
			"orderItemId", _ORDER_ITEM_ID
		).put(
			"projectId", projectId
		).toString();
	}

	private void _verifyNeverDeployed() throws Exception {
		Mockito.verify(
			_cloudAppService, Mockito.never()
		).deployCloudApp(
			ArgumentMatchers.anyLong(), ArgumentMatchers.anyLong(),
			ArgumentMatchers.anyString()
		);
	}

	private void _whenFetchCommerceOrder(Order order) throws Exception {
		Mockito.when(
			_commerceOrderService.fetchCommerceOrder(_ORDER_ID)
		).thenReturn(
			order
		);
	}

	private static final String _DEPLOYMENT_ID = "mock-deployment-1";

	private static final String _EMAIL_ADDRESS = "buyer@example.com";

	private static final long _ORDER_ID = 1000L;

	private static final long _ORDER_ITEM_ID = 2000L;

	private static final int _PAYMENT_STATUS_PENDING = 1;

	private static final String _PROJECT_ID = "omnitest-prd";

	private CloudAppService _cloudAppService;
	private CommerceOrderPermission _commerceOrderPermission;
	private CommerceOrderService _commerceOrderService;
	private ConsoleRestController _consoleRestController;
	private ConsoleService _consoleService;
	private UserAccountService _userAccountService;

}