/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.address.client.dto.v1_0.Country;
import com.liferay.headless.admin.address.client.resource.v1_0.CountryResource;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class CountryService extends OneBaseService {

	public Country getCountryByA2(String a2) throws Exception {
		CountryResource countryResource = CountryResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		return countryResource.getCountryByA2(a2);
	}

}