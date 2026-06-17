/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.portal.kernel.util.StringUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
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
		Collections.addAll(
			_liferayDomains, StringUtil.split(_liferayDomainsString));
	}

	private final Set<String> _liferayDomains = new HashSet<>();

	@Value("${liferay.one.email.address.validator.liferay.domains}")
	private String _liferayDomainsString;

}