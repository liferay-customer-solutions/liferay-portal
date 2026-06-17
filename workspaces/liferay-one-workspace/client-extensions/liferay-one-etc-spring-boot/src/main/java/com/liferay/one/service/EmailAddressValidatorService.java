/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.stereotype.Component;

/**
 * @author Karoline Silva
 */
@Component
public class EmailAddressValidatorService {

	public boolean isLiferayDomain(String emailAddress) {
		String domain = emailAddress.substring(emailAddress.indexOf('@') + 1);

		return _liferayDomains.contains(domain);
	}

	public void validateDomain(String emailAddress) {
		if (isLiferayDomain(emailAddress)) {
			throw new IllegalArgumentException(
				"Email address uses a reserved Liferay domain");
		}
	}

	@PostConstruct
	protected void init() {
		try (InputStream inputStream =
				EmailAddressValidatorService.class.getResourceAsStream(
					"/dependencies/liferay_domains.txt")) {

			new BufferedReader(
				new InputStreamReader(inputStream)
			).lines(
			).filter(
				line -> !line.isBlank()
			).forEach(
				_liferayDomains::add
			);
		}
		catch (Exception exception) {
			_log.error("Unable to load Liferay domains", exception);
		}
	}

	private static final Log _log = LogFactory.getLog(
		EmailAddressValidatorService.class);

	private final Set<String> _liferayDomains = new HashSet<>();

}