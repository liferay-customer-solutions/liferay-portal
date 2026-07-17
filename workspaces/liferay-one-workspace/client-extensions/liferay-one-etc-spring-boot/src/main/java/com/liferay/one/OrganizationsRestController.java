/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.okta.model.OktaUser;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.service.OrganizationService;
import com.liferay.one.service.PropertyService;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ricardo Mariz
 */
@RequestMapping("/organizations")
@RestController
public class OrganizationsRestController extends OneBaseRestController {

	@PostMapping("/{organizationId}/sync-from-okta")
	public void postSyncFromOkta(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable("organizationId") long organizationId)
		throws Exception {

		_adminPermission.check(jwt);

		String oktaGroupId = _propertyService.getPropertyValue(
			Organization.class.getName(), organizationId,
			PropertyConstants.NAME_OKTA_GROUP);

		if (Validator.isNull(oktaGroupId)) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Unable to find an Okta group for organization " +
					organizationId);
		}

		Set<String> oktaEmailAddresses = new HashSet<>();

		for (OktaUser oktaUser : _oktaService.getGroupContacts(oktaGroupId)) {
			String emailAddress = oktaUser.getEmail();

			if (Validator.isNotNull(emailAddress)) {
				oktaEmailAddresses.add(StringUtil.toLowerCase(emailAddress));
			}
		}

		Set<String> organizationEmailAddresses = new HashSet<>();

		for (UserAccount userAccount :
				_userAccountService.getOrganizationUserAccounts(
					organizationId)) {

			String emailAddress = userAccount.getEmailAddress();

			if (Validator.isNotNull(emailAddress)) {
				organizationEmailAddresses.add(
					StringUtil.toLowerCase(emailAddress));
			}
		}

		for (String emailAddress : oktaEmailAddresses) {
			if (!organizationEmailAddresses.contains(emailAddress)) {
				_organizationService.addOrganizationUserAccountByEmailAddress(
					emailAddress, organizationId);
			}
		}

		for (String emailAddress : organizationEmailAddresses) {
			if (!oktaEmailAddresses.contains(emailAddress)) {
				_organizationService.
					removeOrganizationUserAccountByEmailAddress(
						emailAddress, organizationId);
			}
		}
	}

	@Autowired
	private AdminPermission _adminPermission;

	@Autowired
	private OktaService _oktaService;

	@Autowired
	private OrganizationService _organizationService;

	@Autowired
	private PropertyService _propertyService;

	@Autowired
	private UserAccountService _userAccountService;

}