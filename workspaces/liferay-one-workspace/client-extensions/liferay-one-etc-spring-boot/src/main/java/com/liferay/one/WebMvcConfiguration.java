/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.constants.AccountInvitationConstants;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author Ryan Schuhler
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

	@Override
	public void addCorsMappings(CorsRegistry corsRegistry) {
		List<String> allowedOrigins = _getAllowedOrigins();

		if (allowedOrigins.isEmpty()) {
			return;
		}

		corsRegistry.addMapping(
			AccountInvitationConstants.INVITATIONS_PATH + "/**"
		).allowedMethods(
			"GET"
		).allowedOrigins(
			allowedOrigins.toArray(new String[0])
		);
	}

	private List<String> _getAllowedOrigins() {
		List<String> allowedOrigins = new ArrayList<>();

		if (Validator.isNull(_domains)) {
			return allowedOrigins;
		}

		for (String domain : _domains.split("[\\s,]+")) {
			if (Validator.isNotNull(domain)) {
				allowedOrigins.add(_serverProtocol + "://" + domain);
			}
		}

		return allowedOrigins;
	}

	@Value("${com.liferay.lxc.dxp.domains:}")
	private String _domains;

	@Value("${com.liferay.lxc.dxp.server.protocol:https}")
	private String _serverProtocol;

}