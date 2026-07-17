/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.resource.v1_0.OrganizationResource;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Ricardo Mariz
 */
@Component
public class OrganizationService extends OneBaseService {

	public void addOrganizationUserAccountByEmailAddress(
			String emailAddress, long organizationId)
		throws Exception {

		OrganizationResource organizationResource =
			_buildOrganizationResource();

		organizationResource.postUserAccountByEmailAddress(
			String.valueOf(organizationId), emailAddress);
	}

	public void removeOrganizationUserAccountByEmailAddress(
			String emailAddress, long organizationId)
		throws Exception {

		OrganizationResource organizationResource =
			_buildOrganizationResource();

		organizationResource.deleteUserAccountByEmailAddress(
			String.valueOf(organizationId), emailAddress);
	}

	private OrganizationResource _buildOrganizationResource() {
		return OrganizationResource.builder(
		).endpoint(
			lxcDXPMainDomain, lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

}