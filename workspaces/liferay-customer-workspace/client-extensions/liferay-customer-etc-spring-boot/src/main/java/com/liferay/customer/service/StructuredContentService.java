/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.delivery.client.dto.v1_0.StructuredContent;
import com.liferay.headless.delivery.client.pagination.Pagination;
import com.liferay.headless.delivery.client.resource.v1_0.StructuredContentResource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
public class StructuredContentService {

	public List<StructuredContent> getStructuredContent(
			String categoryName, ZonedDateTime zonedDateTime)
		throws Exception {

		String categoryId = _taxonomyCategoryService.getCategoryIdByName(
			categoryName);

		if (zonedDateTime == null) {
			zonedDateTime = ZonedDateTime.now(
				ZoneOffset.UTC
			).minusDays(
				1
			);
		}

		String fromDate = zonedDateTime.withNano(
			0
		).toInstant(
		).toString();

		String filter = String.format(
			"datePublished ge %s and taxonomyCategoryIds/any(t:t eq %s)",
			fromDate, categoryId);

		if (_log.isInfoEnabled()) {
			_log.info("Searching for structured content with filter " + filter);
		}

		StructuredContentResource structuredContentResource =
			StructuredContentResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).build();

		Collection<StructuredContent> structuredContents =
			structuredContentResource.getSiteStructuredContentsPage(
				_siteService.getSiteId(), null, null, null, filter,
				Pagination.of(1, 100), null
			).getItems();

		return new ArrayList<>(structuredContents);
	}

	protected String getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		StructuredContentService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private SiteService _siteService;

	@Autowired
	private TaxonomyCategoryService _taxonomyCategoryService;

}