/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.portal.kernel.util.Validator;

import java.net.URI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * @author Karoline Silva
 */
@Service
public class OAuthTokenUserResolver extends OneBaseService {

	@Cacheable(cacheNames = "oauthTokenUsers", key = "#token")
	public UserAccount resolveUserByOAuthToken(String token) throws Exception {
		try {
			String response = get(
				"Bearer " + token,
				URI.create("/o/headless-admin-user/v1.0/my-user-account"));

			if (Validator.isNull(response)) {
				return null;
			}

			return UserAccount.toDTO(response);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to resolve user by OAuth token", exception);
			}

			return null;
		}
	}

	private static final Log _log = LogFactory.getLog(
		OAuthTokenUserResolver.class);

}