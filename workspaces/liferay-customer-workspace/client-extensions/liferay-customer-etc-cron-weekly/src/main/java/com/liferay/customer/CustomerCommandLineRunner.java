/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.LiferayOAuth2AccessTokenManager;
import com.liferay.petra.string.StringBundler;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.util.HashMapBuilder;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * @author Ryan Schuhler
 */
@Component
@ComponentScan(basePackages = "com.liferay.osb")
public class CustomerCommandLineRunner
	extends BaseRestController implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Running weekly cron job");
		}

		_updateVersionPicklists();

		ConfigurableApplicationContext configurableApplicationContext =
			(ConfigurableApplicationContext)_applicationContext;

		configurableApplicationContext.close();
	}

	private void _addVersionIfAbsentAndSort(
		Map<String, List<String>> versions, String key, String version) {

		List<String> versionList = versions.get(key);

		if ((versionList != null) && !versionList.contains(version)) {
			versionList.add(version);

			Collections.sort(versionList);
		}
	}

	private Map<String, List<String>> _extractVersions(
		JSONArray releasesJSONArray) {

		Map<String, List<String>> versions =
			HashMapBuilder.<String, List<String>>put(
				"dxpMajor", new ArrayList<>()
			).put(
				"dxpMinor", new ArrayList<>()
			).put(
				"portalMajor", new ArrayList<>()
			).put(
				"portalMinor", new ArrayList<>()
			).build();

		for (int i = 0; i < releasesJSONArray.length(); i++) {
			JSONObject releaseJSONObject = releasesJSONArray.getJSONObject(i);

			String product = releaseJSONObject.getString("product");
			String productGroupVersion = releaseJSONObject.getString(
				"productGroupVersion");

			if (Validator.isNull(product) ||
				Validator.isNull(productGroupVersion)) {

				continue;
			}

			String productVersion = releaseJSONObject.getString(
				"productVersion");

			if (product.equals("dxp")) {
				productGroupVersion =
					StringUtil.toUpperCase(product) + StringPool.SPACE +
						productGroupVersion;
			}
			else {
				productGroupVersion =
					StringUtil.getTitleCase(product, false) + StringPool.SPACE +
						productGroupVersion;
			}

			_addVersionIfAbsentAndSort(
				versions, product + "Major", productGroupVersion);
			_addVersionIfAbsentAndSort(
				versions, product + "Minor", productVersion);
		}

		return versions;
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			_liferayOAuthApplicationExternalReferenceCodes);
	}

	private void _updatePicklist(
			String name, String externalReferenceCode, List<String> values)
		throws Exception {

		String listTypeDefinitionsAPI = StringBundler.concat(
			_lxcDXPServerProtocol, "://", _lxcDXPMainDomain,
			_lxcCustomerVersionPicklistsAPI);

		JSONArray listTypeEntriesJSONArray = new JSONArray();

		for (String value : values) {
			if (Validator.isNull(value)) {
				continue;
			}

			JSONObject listTypeEntryJSONObject = new JSONObject();

			JSONObject listTypeEntryNameI18nJSONObject = new JSONObject();

			listTypeEntryNameI18nJSONObject.put("en-US", value);

			listTypeEntryJSONObject.put(
				"externalReferenceCode",
				value.toUpperCase(
				).replaceAll(
					"[^A-Z0-9]", "_"
				)
			).put(
				"key",
				value.toLowerCase(
				).replaceAll(
					"[^a-z0-9]", ""
				)
			).put(
				"name", value
			).put(
				"name_i18n", listTypeEntryNameI18nJSONObject
			);

			listTypeEntriesJSONArray.put(listTypeEntryJSONObject);
		}

		JSONObject transformedPicklistJSONObject = new JSONObject();

		JSONObject nameI18nJSONObject = new JSONObject();

		nameI18nJSONObject.put("en-US", name);

		transformedPicklistJSONObject.put(
			"externalReferenceCode", externalReferenceCode
		).put(
			"listTypeEntries", listTypeEntriesJSONArray
		).put(
			"name", name
		).put(
			"name_i18n", nameI18nJSONObject
		);

		JSONObject listTypeDefinitionJSONObject = new JSONObject(
			get(
				_getAuthorization(),
				listTypeDefinitionsAPI +
					"/list-type-definitions/by-external-reference-code/" +
						externalReferenceCode));

		put(
			_getAuthorization(), transformedPicklistJSONObject.toString(),
			listTypeDefinitionsAPI + "/list-type-definitions/" +
				listTypeDefinitionJSONObject.getInt("id"));

		if (_log.isInfoEnabled()) {
			_log.info("Updated picklists: " + externalReferenceCode);
		}
	}

	private void _updateVersionPicklists() throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Updating version picklists");
		}

		JSONArray releasesJSONArray = new JSONArray(
			get(StringPool.BLANK, _liferayCustomerVersionPicklistsReleasesURL));

		Map<String, List<String>> versions = _extractVersions(
			releasesJSONArray);

		List<String> dxpMajorVersions = versions.get("dxpMajor");
		List<String> dxpMinorVersions = versions.get("dxpMinor");
		List<String> portalMajorVersions = versions.get("portalMajor");
		List<String> portalMinorVersions = versions.get("portalMinor");

		List<String> dxpMinorVersionsAndPortalMajorVersions = new ArrayList<>();

		dxpMinorVersionsAndPortalMajorVersions.addAll(dxpMinorVersions);
		dxpMinorVersionsAndPortalMajorVersions.addAll(portalMajorVersions);

		_updatePicklist(
			"DXP Major Version", _liferayCustomerVersionPicklistsDXPMajorERC,
			dxpMajorVersions);

		_updatePicklist(
			"DXP Minor Version", _liferayCustomerVersionPicklistsDXPMinorERC,
			dxpMinorVersions);

		_updatePicklist(
			"DXP Minor Version and Portal Major Version",
			_liferayCustomerVersionPicklistsDXPMinorPortalMajorERC,
			dxpMinorVersionsAndPortalMajorVersions);

		_updatePicklist(
			"Portal Major Version",
			_liferayCustomerVersionPicklistsPortalMajorERC,
			portalMajorVersions);

		_updatePicklist(
			"Portal Minor Version",
			_liferayCustomerVersionPicklistsPortalMinorERC,
			portalMinorVersions);
	}

	private static final Log _log = LogFactory.getLog(
		CustomerCommandLineRunner.class);

	@Autowired
	private ApplicationContext _applicationContext;

	@Value("${liferay.customer.version.picklists.dxp.major.erc}")
	private String _liferayCustomerVersionPicklistsDXPMajorERC;

	@Value("${liferay.customer.version.picklists.dxp.minor.erc}")
	private String _liferayCustomerVersionPicklistsDXPMinorERC;

	@Value("${liferay.customer.version.picklists.dxp.minor.portal.major.erc}")
	private String _liferayCustomerVersionPicklistsDXPMinorPortalMajorERC;

	@Value("${liferay.customer.version.picklists.portal.major.erc}")
	private String _liferayCustomerVersionPicklistsPortalMajorERC;

	@Value("${liferay.customer.version.picklists.portal.minor.erc}")
	private String _liferayCustomerVersionPicklistsPortalMinorERC;

	@Value("${liferay.customer.version.picklists.releases.url}")
	private String _liferayCustomerVersionPicklistsReleasesURL;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${liferay.oauth.application.external.reference.codes}")
	private String _liferayOAuthApplicationExternalReferenceCodes;

	@Value("${liferay.customer.version.picklists.api}")
	private String _lxcCustomerVersionPicklistsAPI;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

}