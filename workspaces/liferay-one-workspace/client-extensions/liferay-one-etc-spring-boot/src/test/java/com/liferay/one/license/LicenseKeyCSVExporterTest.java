/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.license;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.model.AccountSupportInfo;
import com.liferay.one.model.LicenseKey;
import com.liferay.one.service.AccountService;
import com.liferay.one.service.CommerceOrderService;
import com.liferay.one.service.EntitlementService;
import com.liferay.petra.string.CharPool;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.ee.license.shared.LicenseConstants;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Allen Ziegenfus
 */
public class LicenseKeyCSVExporterTest {

	@BeforeEach
	public void setUp() throws Exception {
		_licenseKeyCSVExporter = new LicenseKeyCSVExporter();

		_accountService = Mockito.mock(AccountService.class);
		_commerceOrderService = Mockito.mock(CommerceOrderService.class);
		_entitlementService = Mockito.mock(EntitlementService.class);

		ReflectionTestUtils.setField(
			_licenseKeyCSVExporter, "_accountService", _accountService);
		ReflectionTestUtils.setField(
			_licenseKeyCSVExporter, "_commerceOrderService",
			_commerceOrderService);
		ReflectionTestUtils.setField(
			_licenseKeyCSVExporter, "_entitlementService", _entitlementService);
	}

	@Test
	public void testGetFileName() {
		Assertions.assertEquals(
			"activation-key-details.csv", _licenseKeyCSVExporter.getFileName());
	}

	@Test
	public void testToCSV() throws Exception {
		Account account = new Account();

		account.setDefaultBillingAddressId(4L);
		account.setExternalReferenceCode("ACCNT-1");
		account.setId(_ACCOUNT_ENTRY_ID);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			account
		);

		Mockito.when(
			_commerceOrderService.getAccountSupportInfo(_ACCOUNT_ENTRY_ID, 4L)
		).thenReturn(
			new AccountSupportInfo("en_US", "US")
		);

		Mockito.when(
			_entitlementService.getSubscriptionState(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			"Active"
		);

		String csv = _licenseKeyCSVExporter.toCSV(
			Collections.singletonList(_createLicenseKey("enterprise")));

		List<String> lines = List.of(csv.split("\n"));

		Assertions.assertEquals(2, lines.size());

		Assertions.assertEquals(
			StringBundler.concat(
				"Project Name,Account Key,Project State,Support Region,",
				"Product Version,Product Name,License Key Id,IP Addresses,",
				"MAC Addresses,Host Name,Instance Sizing,License Start Date,",
				"License Expiration Date,License Status,Max Servers/Cluster ",
				"Nodes,Complimentary"),
			lines.get(0));

		Assertions.assertEquals(
			"\"Acme\",\"ACCNT-1\",\"Active\",\"US\",\"7.4 U100\",\"DXP\"," +
				"\"12\",\"1.2.3.4\",\"AA:BB\",\"acme.host\",\"4\",\"null\"," +
					"\"null\",\"Active\",\"9\",\"false\"",
			lines.get(1));
	}

	@Test
	public void testToCSVEscapesQuotes() throws Exception {
		Account account = new Account();

		account.setDefaultBillingAddressId(4L);
		account.setExternalReferenceCode("ACCNT-1");
		account.setId(_ACCOUNT_ENTRY_ID);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			account
		);

		Mockito.when(
			_commerceOrderService.getAccountSupportInfo(_ACCOUNT_ENTRY_ID, 4L)
		).thenReturn(
			new AccountSupportInfo("en_US", "US")
		);

		Mockito.when(
			_entitlementService.getSubscriptionState(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			"Active"
		);

		LicenseKey licenseKey = _createLicenseKey("enterprise");

		Mockito.when(
			licenseKey.getHostName()
		).thenReturn(
			"say \"hello\", friend"
		);

		Mockito.when(
			licenseKey.getIpAddresses()
		).thenReturn(
			"=1+1"
		);

		String csv = _licenseKeyCSVExporter.toCSV(
			Collections.singletonList(licenseKey));

		List<String> lines = List.of(csv.split("\n"));

		Assertions.assertEquals(2, lines.size());

		Assertions.assertTrue(
			lines.get(
				1
			).contains(
				"\"say \"\"hello\"\", friend\""
			),
			lines.get(1));

		Assertions.assertTrue(
			lines.get(
				1
			).contains(
				"\"'=1+1\""
			),
			lines.get(1));

		Assertions.assertEquals(
			16, StringUtil.count(lines.get(0), CharPool.COMMA) + 1);
	}

	@Test
	public void testToCSVResolvesEachAccountOnce() throws Exception {
		Account account = new Account();

		account.setDefaultBillingAddressId(4L);
		account.setExternalReferenceCode("ACCNT-1");
		account.setId(_ACCOUNT_ENTRY_ID);

		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			account
		);

		Mockito.when(
			_commerceOrderService.getAccountSupportInfo(_ACCOUNT_ENTRY_ID, 4L)
		).thenReturn(
			new AccountSupportInfo("en_US", "US")
		);

		Mockito.when(
			_entitlementService.getSubscriptionState(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			"Active"
		);

		String csv = _licenseKeyCSVExporter.toCSV(
			List.of(
				_createLicenseKey("enterprise"),
				_createLicenseKey("enterprise"),
				_createLicenseKey("enterprise")));

		List<String> lines = List.of(csv.split("\n"));

		Assertions.assertEquals(4, lines.size());

		Mockito.verify(
			_accountService, Mockito.times(1)
		).fetchAccount(
			_ACCOUNT_ENTRY_ID
		);

		Mockito.verify(
			_commerceOrderService, Mockito.times(1)
		).getAccountSupportInfo(
			_ACCOUNT_ENTRY_ID, 4L
		);

		Mockito.verify(
			_entitlementService, Mockito.times(1)
		).getSubscriptionState(
			_ACCOUNT_ENTRY_ID
		);
	}

	@Test
	public void testToCSVWhenAccountIsNull() throws Exception {
		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			null
		);

		Mockito.when(
			_entitlementService.getSubscriptionState(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			""
		);

		String csv = _licenseKeyCSVExporter.toCSV(
			Collections.singletonList(_createLicenseKey("enterprise")));

		List<String> lines = List.of(csv.split("\n"));

		Assertions.assertEquals(
			"\"Acme\",\"\",\"\",\"\",\"7.4 U100\",\"DXP\",\"12\",\"1.2.3.4\"," +
				"\"AA:BB\",\"acme.host\",\"4\",\"null\",\"null\",\"Active\"," +
					"\"9\",\"false\"",
			lines.get(1));

		Mockito.verifyNoInteractions(_commerceOrderService);
	}

	@Test
	public void testToCSVWhenLicenseTypeIsVirtualCluster() throws Exception {
		Mockito.when(
			_accountService.fetchAccount(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			null
		);

		Mockito.when(
			_entitlementService.getSubscriptionState(_ACCOUNT_ENTRY_ID)
		).thenReturn(
			""
		);

		String csv = _licenseKeyCSVExporter.toCSV(
			Collections.singletonList(
				_createLicenseKey(LicenseConstants.TYPE_VIRTUAL_CLUSTER)));

		Assertions.assertTrue(csv.contains("\"Active\",\"3\",\"false\""));
	}

	private LicenseKey _createLicenseKey(String licenseType) {
		LicenseKey licenseKey = Mockito.mock(LicenseKey.class);

		Mockito.when(
			licenseKey.getAccountEntryId()
		).thenReturn(
			_ACCOUNT_ENTRY_ID
		);

		Mockito.when(
			licenseKey.getAccountName()
		).thenReturn(
			"Acme"
		);

		Mockito.when(
			licenseKey.getHostName()
		).thenReturn(
			"acme.host"
		);

		Mockito.when(
			licenseKey.getIpAddresses()
		).thenReturn(
			"1.2.3.4"
		);

		Mockito.when(
			licenseKey.getLicenseKeyId()
		).thenReturn(
			12L
		);

		Mockito.when(
			licenseKey.getLicenseType()
		).thenReturn(
			licenseType
		);

		Mockito.when(
			licenseKey.getMacAddresses()
		).thenReturn(
			"AA:BB"
		);

		Mockito.when(
			licenseKey.getMaxClusterNodes()
		).thenReturn(
			3
		);

		Mockito.when(
			licenseKey.getMaxServers()
		).thenReturn(
			9
		);

		Mockito.when(
			licenseKey.getProductName()
		).thenReturn(
			"DXP"
		);

		Mockito.when(
			licenseKey.getProductVersionLabel()
		).thenReturn(
			"7.4 U100"
		);

		Mockito.when(
			licenseKey.getSizing()
		).thenReturn(
			"4"
		);

		Mockito.when(
			licenseKey.isActive()
		).thenReturn(
			true
		);

		return licenseKey;
	}

	private static final long _ACCOUNT_ENTRY_ID = 55L;

	private AccountService _accountService;
	private CommerceOrderService _commerceOrderService;
	private EntitlementService _entitlementService;
	private LicenseKeyCSVExporter _licenseKeyCSVExporter;

}