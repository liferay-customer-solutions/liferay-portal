/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.osb.provisioning.rest.client.dto.v1_0.LicenseKey;
import com.liferay.osb.provisioning.rest.client.pagination.Page;
import com.liferay.osb.provisioning.rest.client.pagination.Pagination;
import com.liferay.osb.provisioning.rest.client.resource.v1_0.LicenseKeyResource;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.StringBundler;

import java.net.URL;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Ryan Schuhler
 */
@Component
public class ProvisioningService {

	public List<LicenseKey> checkLicenseExpirations() {
		if (_log.isInfoEnabled()) {
			_log.info("Checking for license expirations");
		}

		try {
			LicenseKeyResource licenseKeyResource = LicenseKeyResource.builder(
			).bearerToken(
				_liferayOAuth2AccessTokenManager.getOAuth2AccessToken(
					_oauth2ClientId
				).getTokenValue()
			).endpoint(
				new URL(
					_provisioningURL
				).getHost(),
				new URL(
					_provisioningURL
				).getPort(),
				new URL(
					_provisioningURL
				).getProtocol()
			).build();

			Page<LicenseKey> licenseKeyPage =
				licenseKeyResource.getAccountAccountKeyLicenseKeysPage(
					null, null, null, Pagination.of(0, 100), null);

			if ((licenseKeyPage != null) &&
				(licenseKeyPage.getItems() != null)) {

				List<String> expiringLicenses = new ArrayList<>();

				for (LicenseKey license : licenseKeyPage.getItems()) {
					Date expirationDateUtil = license.getExpirationDate();

					String expirationDateStr =
						DateTimeFormatter.ISO_LOCAL_DATE.format(
							expirationDateUtil.toInstant(
							).atZone(
								ZoneId.systemDefault()
							).toLocalDate());

					LocalDate expirationDate = LocalDate.parse(
						expirationDateStr, DateTimeFormatter.ISO_LOCAL_DATE);

					LocalDate now = LocalDate.now();

					if (expirationDate.isAfter(now) &&
						expirationDate.isBefore(now.plusDays(30))) {

						expiringLicenses.add(
							StringBundler.concat(
								license.getName(), " (Expires: ",
								expirationDateStr, ")"));
					}
				}

				if (!expiringLicenses.isEmpty()) {
					_log.info(
						"Expiring Licenses: " + expiringLicenses.toString());
				}
				else {
					_log.info("No licenses expiring soon");
				}

				return new ArrayList<>(licenseKeyPage.getItems());
			}

			return Collections.emptyList();
		}
		catch (Exception exception) {
			throw new SystemException(
				"Error checking license expiration", exception);
		}
	}

	private static final Log _log = LogFactory.getLog(
		ProvisioningService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${liferay.customer.provisioning.oauth.client.id}")
	private String _oauth2ClientId;

	@Value("${liferay.customer.provisioning.oauth.client.secret}")
	private String _oauth2ClientSecret;

	@Value("${liferay.customer.provisioning.url}")
	private String _provisioningURL;

}