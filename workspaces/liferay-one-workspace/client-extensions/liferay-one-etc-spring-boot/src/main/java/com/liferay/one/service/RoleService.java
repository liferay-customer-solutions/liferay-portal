/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Role;
import com.liferay.headless.admin.user.client.pagination.Page;
import com.liferay.headless.admin.user.client.pagination.Pagination;
import com.liferay.headless.admin.user.client.resource.v1_0.RoleResource;
import com.liferay.portal.kernel.model.role.RoleConstants;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Drew Brokke
 */
@Component
public class RoleService extends OneBaseService {

	public void addOrganizationUserAccountRole(
			long organizationId, long roleId, long userId)
		throws Exception {

		RoleResource roleResource = _buildRoleResource();

		roleResource.postOrganizationRoleUserAccountAssociation(
			roleId, userId, organizationId);
	}

	public List<Role> getAccountRoles() throws Exception {
		return _getRoles(RoleConstants.TYPE_ACCOUNT);
	}

	public List<Role> getOrganizationRoles() throws Exception {
		return _getRoles(RoleConstants.TYPE_ORGANIZATION);
	}

	public Role getRole(long roleId) throws Exception {
		RoleResource roleResource = _buildRoleResource();

		return roleResource.getRole(roleId);
	}

	public void removeOrganizationUserAccountRole(
			long organizationId, long roleId, long userId)
		throws Exception {

		RoleResource roleResource = _buildRoleResource();

		roleResource.deleteOrganizationRoleUserAccountAssociation(
			roleId, userId, organizationId);
	}

	private RoleResource _buildRoleResource() {
		return RoleResource.builder(
		).endpoint(
			getDXPEndpointAddress(), lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();
	}

	private List<Role> _getRoles(int type) throws Exception {
		List<Role> roles = new ArrayList<>();

		RoleResource roleResource = _buildRoleResource();

		int page = 1;

		while (true) {
			Page<Role> rolesPage = roleResource.getRolesPage(
				null, new Integer[] {type}, null,
				Pagination.of(page, _PAGE_SIZE));

			roles.addAll(rolesPage.getItems());

			if (page >= rolesPage.getLastPage()) {
				break;
			}

			page++;
		}

		return roles;
	}

	private static final int _PAGE_SIZE = 500;

}