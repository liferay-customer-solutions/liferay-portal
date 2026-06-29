/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.PostalAddress;
import com.liferay.headless.admin.user.client.resource.v1_0.PostalAddressResource;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Felipe Veloso
 */
@Component
public class PostalAddressService extends OneBaseService {

	public PostalAddress getPostalAddress(long postalAddressId)
		throws Exception {

		PostalAddressResource postalAddressResource =
			PostalAddressResource.builder(
			).endpoint(
				lxcDXPMainDomain, lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).build();

		return postalAddressResource.getPostalAddress(postalAddressId);
	}

}