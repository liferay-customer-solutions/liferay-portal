/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.admin.user.client.dto.v1_0.Site;
import com.liferay.headless.admin.user.client.resource.v1_0.SiteResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * @author Ryan Schuhler
 */
@Component
public class SiteService {

	public Long getSiteId() throws Exception {
		if (_siteId != null) {
			return _siteId;
		}

		if (_log.isInfoEnabled()) {
			_log.info(
				"Searching for site with friendly URL path: " +
					_friendlyUrlPath);
		}

		SiteResource.Builder builder = SiteResource.builder();

		SiteResource siteResource = builder.endpoint(
			_lxcDXPMainDomain, _lxcDXPServerProtocol
		).header(
			HttpHeaders.AUTHORIZATION, getAuthorization()
		).build();

		Site site = siteResource.getSiteByFriendlyUrlPath(_friendlyUrlPath);

		if (site != null) {
			_siteId = site.getId();

			return _siteId;
		}

		throw new Exception("Site not found: " + _friendlyUrlPath);
	}

	protected String getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(SiteService.class);

	@Value("${liferay.customer.portal.friendly.url.path}")
	private String _friendlyUrlPath;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	private Long _siteId;

}