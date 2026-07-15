/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Account;
import com.liferay.headless.commerce.admin.order.client.dto.v1_0.Order;
import com.liferay.one.constants.ClassNameConstants;
import com.liferay.one.constants.CommerceOrderConstants;
import com.liferay.one.model.LicenseKey;
import com.liferay.one.model.SubscriptionEntry;
import com.liferay.one.permission.LicenseKeyPermission;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.LicenseKeyService;
import com.liferay.one.service.SubscriptionEntryService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;
import com.liferay.portal.kernel.security.permission.ActionKeys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Amos Fong
 */
public class LicenseKeysRestControllerTest {

	@Test
	public void testDeleteSubscriptions() throws Exception {
		LicenseKeysRestController licenseKeysRestController =
			_createController();

		licenseKeysRestController.deleteSubscriptions(
			null, new long[] {1L, 2L});

		Mockito.verify(
			_subscriptionEntryService
		).deleteSubscriptionEntry(
			null, ClassNameConstants.LICENSE_KEY, 1L, _USER_ID
		);

		Mockito.verify(
			_subscriptionEntryService
		).deleteSubscriptionEntry(
			null, ClassNameConstants.LICENSE_KEY, 2L, _USER_ID
		);
	}

	@Test
	public void testGetLicenseKeysDownload() throws Exception {
		LicenseKeysRestController licenseKeysRestController =
			_createController();

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getAccountEntryId()
		).thenReturn(
			_ACCOUNT_ID
		);

		Mockito.when(
			licenseKey.getLicenseVersion()
		).thenReturn(
			3
		);

		Mockito.when(
			_licenseKeyService.getLicenseKey(Mockito.any(), Mockito.anyLong())
		).thenReturn(
			licenseKey
		);

		Mockito.when(
			_licenseKeyService.getLicenseKeyDownloadFileName(licenseKey)
		).thenReturn(
			"activation-key.xml"
		);

		Mockito.when(
			_licenseKeyService.getLicenseKeyDownloadXML(licenseKey)
		).thenReturn(
			"<license/>"
		);

		ResponseEntity<String> responseEntity =
			licenseKeysRestController.getLicenseKeysDownload(null, 1L);

		Assertions.assertEquals("<license/>", responseEntity.getBody());

		HttpHeaders httpHeaders = responseEntity.getHeaders();

		Assertions.assertEquals(
			"attachment; filename=\"activation-key.xml\"",
			httpHeaders.getFirst(HttpHeaders.CONTENT_DISPOSITION));
		Assertions.assertEquals(
			MediaType.APPLICATION_XML, httpHeaders.getContentType());
	}

	@Test
	public void testGetLicenseKeysDownloadThrowsNotFoundForOldVersion()
		throws Exception {

		LicenseKeysRestController licenseKeysRestController =
			_createController();

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getAccountEntryId()
		).thenReturn(
			_ACCOUNT_ID
		);

		Mockito.when(
			licenseKey.getLicenseVersion()
		).thenReturn(
			1
		);

		Mockito.when(
			_licenseKeyService.getLicenseKey(Mockito.any(), Mockito.anyLong())
		).thenReturn(
			licenseKey
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() -> licenseKeysRestController.getLicenseKeysDownload(
					null, 1L));

		Assertions.assertEquals(
			HttpStatus.NOT_FOUND, responseStatusException.getStatusCode());
	}

	@Test
	public void testGetSubscriptionsReturnsFalseWhenNotSubscribed()
		throws Exception {

		LicenseKeysRestController licenseKeysRestController =
			_createController();

		Mockito.when(
			_subscriptionEntryService.fetchSubscriptionEntry(
				null, ClassNameConstants.LICENSE_KEY, 5L, _USER_ID)
		).thenReturn(
			null
		);

		Assertions.assertFalse(
			licenseKeysRestController.getSubscriptions(null, 5L));
	}

	@Test
	public void testGetSubscriptionsReturnsTrueWhenSubscribed()
		throws Exception {

		LicenseKeysRestController licenseKeysRestController =
			_createController();

		Mockito.when(
			_subscriptionEntryService.fetchSubscriptionEntry(
				null, ClassNameConstants.LICENSE_KEY, 5L, _USER_ID)
		).thenReturn(
			Mockito.mock(SubscriptionEntry.class)
		);

		Assertions.assertTrue(
			licenseKeysRestController.getSubscriptions(null, 5L));
	}

	@Test
	public void testPostLicenseKeysTypeFree() throws Exception {
		LicenseKeysRestController licenseKeysRestController =
			_createController();

		Mockito.when(
			_licenseKeyService.hasLicenseKeyTypeFree(
				"example.com", "owner@example.com")
		).thenReturn(
			false
		);

		Account account = Mockito.mock(Account.class);

		Mockito.when(
			account.getId()
		).thenReturn(
			_ACCOUNT_ID
		);

		Order order = Mockito.mock(Order.class);

		Mockito.when(
			order.getAccount()
		).thenReturn(
			account
		);

		Mockito.when(
			order.getId()
		).thenReturn(
			999L
		);

		Mockito.when(
			order.getOrderStatus()
		).thenReturn(
			CommerceOrderConstants.ORDER_STATUS_OPEN
		);

		Mockito.when(
			_commerceOrderService.getCommerceOrder(999L)
		).thenReturn(
			order
		);

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			_licenseKeyService.addLicenseKeyTypeFree(
				_ACCOUNT_ID, "example.com", "999", "owner@example.com")
		).thenReturn(
			licenseKey
		);

		Assertions.assertSame(
			licenseKey,
			licenseKeysRestController.postLicenseKeysTypeFree(
				"{\"domains\": \"example.com\", \"orderId\": \"999\", " +
					"\"owner\": \"owner@example.com\"}"));

		Mockito.verify(
			_commerceOrderService
		).completeOrder(
			999L, CommerceOrderConstants.ORDER_PAYMENT_STATUS_NOT_REQUIRED
		);
	}

	@Test
	public void testPostLicenseKeysTypeFreeDomainsCheckThrowsConflictWhenDomainExists()
		throws Exception {

		LicenseKeysRestController licenseKeysRestController =
			_createController();

		Mockito.when(
			_licenseKeyService.hasLicenseKeyTypeFree(
				"example.com", "owner@example.com")
		).thenReturn(
			true
		);

		ResponseStatusException responseStatusException =
			Assertions.assertThrows(
				ResponseStatusException.class,
				() ->
					licenseKeysRestController.
						postLicenseKeysTypeFreeDomainsCheck(
							"{\"domains\": \"example.com\", \"owner\": " +
								"\"owner@example.com\"}"));

		Assertions.assertEquals(
			HttpStatus.CONFLICT, responseStatusException.getStatusCode());
	}

	@Test
	public void testPutSubscriptions() throws Exception {
		LicenseKeysRestController licenseKeysRestController =
			_createController();

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getAccountEntryId()
		).thenReturn(
			_ACCOUNT_ID
		);

		Mockito.when(
			_licenseKeyService.getLicenseKey(Mockito.any(), Mockito.anyLong())
		).thenReturn(
			licenseKey
		);

		licenseKeysRestController.putSubscriptions(null, new long[] {1L, 2L});

		Mockito.verify(
			_licenseKeyService
		).getLicenseKey(
			null, 1L
		);

		Mockito.verify(
			_licenseKeyService
		).getLicenseKey(
			null, 2L
		);

		Mockito.verify(
			_licenseKeyPermission, Mockito.times(2)
		).check(
			_ACCOUNT_ID, ActionKeys.VIEW, null
		);

		Mockito.verify(
			_subscriptionEntryService
		).addSubscriptionEntry(
			null, ClassNameConstants.LICENSE_KEY, 1L, _USER_ID
		);

		Mockito.verify(
			_subscriptionEntryService
		).addSubscriptionEntry(
			null, ClassNameConstants.LICENSE_KEY, 2L, _USER_ID
		);
	}

	@Test
	public void testPutSubscriptionsThrowsForbiddenWhenAccountNotViewable()
		throws Exception {

		LicenseKeysRestController licenseKeysRestController =
			_createController();

		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getAccountEntryId()
		).thenReturn(
			_ACCOUNT_ID
		);

		Mockito.when(
			_licenseKeyService.getLicenseKey(Mockito.any(), Mockito.anyLong())
		).thenReturn(
			licenseKey
		);

		Mockito.doThrow(
			new PrincipalException()
		).when(
			_licenseKeyPermission
		).check(
			_ACCOUNT_ID, ActionKeys.VIEW, null
		);

		Assertions.assertThrows(
			PrincipalException.class,
			() -> licenseKeysRestController.putSubscriptions(
				null, new long[] {1L}));

		Mockito.verify(
			_subscriptionEntryService, Mockito.never()
		).addSubscriptionEntry(
			Mockito.any(), Mockito.anyString(), Mockito.anyLong(),
			Mockito.anyLong()
		);
	}

	private LicenseKeysRestController _createController() throws Exception {
		LicenseKeysRestController licenseKeysRestController =
			new LicenseKeysRestController();

		UserAccount userAccount = Mockito.mock(UserAccount.class);

		Mockito.when(
			userAccount.getId()
		).thenReturn(
			_USER_ID
		);

		UserAccountService userAccountService = Mockito.mock(
			UserAccountService.class);

		Mockito.when(
			userAccountService.getMyUserAccount(Mockito.any())
		).thenReturn(
			userAccount
		);

		ReflectionTestUtils.setField(
			licenseKeysRestController, "_commerceOrderService",
			_commerceOrderService);

		ReflectionTestUtils.setField(
			licenseKeysRestController, "_licenseKeyPermission",
			_licenseKeyPermission);

		ReflectionTestUtils.setField(
			licenseKeysRestController, "_licenseKeyService",
			_licenseKeyService);

		ReflectionTestUtils.setField(
			licenseKeysRestController, "_subscriptionEntryService",
			_subscriptionEntryService);

		ReflectionTestUtils.setField(
			licenseKeysRestController, "_userAccountService",
			userAccountService);

		return licenseKeysRestController;
	}

	private static final long _ACCOUNT_ID = 555L;

	private static final long _USER_ID = 123L;

	private final CommerceOrderService _commerceOrderService = Mockito.mock(
		CommerceOrderService.class);
	private final LicenseKeyPermission _licenseKeyPermission = Mockito.mock(
		LicenseKeyPermission.class);
	private final LicenseKeyService _licenseKeyService = Mockito.mock(
		LicenseKeyService.class);
	private final SubscriptionEntryService _subscriptionEntryService =
		Mockito.mock(SubscriptionEntryService.class);

}