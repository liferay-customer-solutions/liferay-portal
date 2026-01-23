/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.service;

import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.headless.admin.taxonomy.client.dto.v1_0.TaxonomyCategory;
import com.liferay.headless.admin.taxonomy.client.pagination.Pagination;
import com.liferay.headless.admin.taxonomy.client.resource.v1_0.TaxonomyCategoryResource;

import java.util.Collection;

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
public class TaxonomyCategoryService {

	public String getCategoryIdByName(String categoryName) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Searching for category: " + categoryName);
		}

		TaxonomyCategoryResource taxonomyCategoryResource =
			TaxonomyCategoryResource.builder(
			).endpoint(
				_lxcDXPMainDomain, _lxcDXPServerProtocol
			).header(
				HttpHeaders.AUTHORIZATION, getAuthorization()
			).build();

		Collection<TaxonomyCategory> taxonomyCategories =
			taxonomyCategoryResource.getSiteTaxonomyCategoriesPage(
				_siteService.getSiteId(), null, null,
				"name eq '" + categoryName + "'", Pagination.of(1, 1), null
			).getItems();

		if (!taxonomyCategories.isEmpty()) {
			return taxonomyCategories.iterator(
			).next(
			).getId();
		}

		throw new Exception("Category not found: " + categoryName);
	}

	protected String getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-spring-boot-oahs");
	}

	private static final Log _log = LogFactory.getLog(
		TaxonomyCategoryService.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Autowired
	private SiteService _siteService;

}