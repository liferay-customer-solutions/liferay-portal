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
import com.liferay.petra.string.StringPool;
import com.liferay.portal.ee.license.shared.LicenseConstants;
import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Allen Ziegenfus
 */
@Component
public class LicenseKeyCSVExporter {

	public String getFileName() {
		return "activation-key-details.csv";
	}

	public String toCSV(Collection<LicenseKey> licenseKeys) throws Exception {
		StringBundler sb = new StringBundler(1 + licenseKeys.size());

		sb.append(_HEADER);

		Map<Long, String> externalReferenceCodes = new HashMap<>();
		Map<Long, String> subscriptionStates = new HashMap<>();
		Map<Long, String> supportRegions = new HashMap<>();

		for (LicenseKey licenseKey : licenseKeys) {
			long accountEntryId = licenseKey.getAccountEntryId();

			if (!externalReferenceCodes.containsKey(accountEntryId)) {
				Account account = _accountService.fetchAccount(accountEntryId);

				externalReferenceCodes.put(
					accountEntryId, _getExternalReferenceCode(account));
				supportRegions.put(accountEntryId, _getSupportRegion(account));

				subscriptionStates.put(
					accountEntryId,
					_entitlementService.getSubscriptionState(accountEntryId));
			}

			sb.append(
				_formatCSVFields(
					licenseKey.getAccountName(),
					externalReferenceCodes.get(accountEntryId),
					subscriptionStates.get(accountEntryId),
					supportRegions.get(accountEntryId),
					licenseKey.getProductVersionLabel(),
					licenseKey.getProductName(), licenseKey.getLicenseKeyId(),
					licenseKey.getIpAddresses(), licenseKey.getMacAddresses(),
					licenseKey.getHostName(), licenseKey.getSizing(),
					licenseKey.getStartDateInstant(),
					licenseKey.getCustomExpirationDateInstant(),
					_getStatus(licenseKey), _getMaxServersOrNodes(licenseKey),
					licenseKey.isComplimentary()));
		}

		return sb.toString();
	}

	private String _escapeCSVValue(Object object) {
		return StringUtil.replace(
			String.valueOf(object), CharPool.QUOTE, "\"\"");
	}

	private String _formatCSVFields(Object... objects) {
		StringBundler sb = new StringBundler((4 * objects.length) + 1);

		for (int i = 0; i < objects.length; i++) {
			sb.append(StringPool.QUOTE);
			sb.append(_escapeCSVValue(objects[i]));
			sb.append(StringPool.QUOTE);

			if (i < (objects.length - 1)) {
				sb.append(StringPool.COMMA);
			}
		}

		sb.append(StringPool.NEW_LINE);

		return sb.toString();
	}

	private String _getExternalReferenceCode(Account account) {
		if (account == null) {
			return StringPool.BLANK;
		}

		return account.getExternalReferenceCode();
	}

	private int _getMaxServersOrNodes(LicenseKey licenseKey) {
		if (StringUtil.equals(
				licenseKey.getLicenseType(),
				LicenseConstants.TYPE_VIRTUAL_CLUSTER)) {

			return licenseKey.getMaxClusterNodes();
		}

		return licenseKey.getMaxServers();
	}

	private String _getStatus(LicenseKey licenseKey) {
		if (licenseKey.isActive()) {
			return "Active";
		}

		return "Inactive";
	}

	private String _getSupportRegion(Account account) throws Exception {
		if (account == null) {
			return StringPool.BLANK;
		}

		AccountSupportInfo accountSupportInfo =
			_commerceOrderService.getAccountSupportInfo(
				account.getId(), account.getDefaultBillingAddressId());

		if (accountSupportInfo == null) {
			return StringPool.BLANK;
		}

		return accountSupportInfo.getSupportRegion();
	}

	private static final String _HEADER = StringBundler.concat(
		"Project Name,Account Key,Project State,Support Region,Product ",
		"Version,Product Name,License Key Id,IP Addresses,MAC Addresses,",
		"Host Name,Instance Sizing,License Start Date,License Expiration ",
		"Date,License Status,Max Servers/Cluster Nodes,Complimentary\n");

	@Autowired
	private AccountService _accountService;

	@Autowired
	private CommerceOrderService _commerceOrderService;

	@Autowired
	private EntitlementService _entitlementService;

}