/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import javax.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @author Karoline Silva
 */
@ConfigurationProperties("okta")
@Validated
public class OktaProperties {

	@NotBlank
	public String getApiToken() {
		return _apiToken;
	}

	@NotBlank
	public String getHost() {
		return _host;
	}

	public void setApiToken(String apiToken) {
		_apiToken = apiToken;
	}

	public void setHost(String host) {
		_host = host;
	}

	private String _apiToken;
	private String _host;

}