/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.headless.admin.user.client.dto.v1_0.Account;
import com.liferay.one.constants.PropertyConstants;
import com.liferay.one.model.Property;
import com.liferay.one.okta.pubsub.OktaPubsubPublisher;
import com.liferay.one.pubsub.Message;
import com.liferay.one.salesforce.model.OpportunityLineItem;
import com.liferay.petra.string.StringBundler;
import com.liferay.portal.kernel.util.Validator;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Kyle Bischof
 */
@Component
public class ProvisioningSubdomainService {

	public void provisionSubdomain(
			Account account, List<OpportunityLineItem> opportunityLineItems)
		throws Exception {

		boolean paasExperience = false;

		for (OpportunityLineItem opportunityLineItem : opportunityLineItems) {
			if (Objects.equals(
					opportunityLineItem.getProductName(),
					_PRODUCT_NAME_PAAS_EXPERIENCE)) {

				paasExperience = true;

				break;
			}
		}

		if (!paasExperience) {
			return;
		}

		String existingSubdomain = _propertyService.getPropertyValue(
			account.getId(), PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN);

		if (Validator.isNotNull(existingSubdomain)) {
			return;
		}

		String subdomain = _generateUniqueSubdomain();

		if (subdomain == null) {
			if (_log.isWarnEnabled()) {
				_log.warn(
					StringBundler.concat(
						"Unable to generate a unique cloud native subdomain ",
						"after ", _SUBDOMAIN_MAX_ATTEMPTS, " attempts"));
			}

			return;
		}

		_propertyService.addAccountProperty(
			account.getId(), PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN,
			subdomain);

		try {
			_oktaPubsubPublisher.publish(
				new Message(
					null,
					new JSONObject(
					).put(
						"accountKey", account.getExternalReferenceCode()
					).put(
						"subdomain", subdomain
					).toString(),
					"okta-app-create"));
		}
		catch (Exception exception) {
			_log.error(
				"Unable to publish the Okta application creation message for " +
					"account " + account.getExternalReferenceCode(),
				exception);
		}
	}

	private String _generateSubdomain() {
		char[] characters = new char[_SUBDOMAIN_LENGTH];

		for (int i = 0; i < _SUBDOMAIN_LENGTH; i++) {
			characters[i] = _SUBDOMAIN_CHARACTERS.charAt(
				ThreadLocalRandom.current(
				).nextInt(
					_SUBDOMAIN_CHARACTERS.length()
				));
		}

		return new String(characters);
	}

	private String _generateUniqueSubdomain() throws Exception {
		for (int i = 0; i < _SUBDOMAIN_MAX_ATTEMPTS; i++) {
			String subdomain = _generateSubdomain();

			List<Property> properties = _propertyService.getProperties(
				StringBundler.concat(
					"(name eq '", PropertyConstants.NAME_CLOUD_NATIVE_SUBDOMAIN,
					"') and (value eq '", subdomain, "')"));

			if (properties.isEmpty()) {
				return subdomain;
			}
		}

		return null;
	}

	private static final String _PRODUCT_NAME_PAAS_EXPERIENCE =
		"PaaS Experience";

	private static final String _SUBDOMAIN_CHARACTERS =
		"abcdefghijklmnopqrstuvwxyz";

	private static final int _SUBDOMAIN_LENGTH = 8;

	private static final int _SUBDOMAIN_MAX_ATTEMPTS = 10;

	private static final Log _log = LogFactory.getLog(
		ProvisioningSubdomainService.class);

	@Autowired
	private OktaPubsubPublisher _oktaPubsubPublisher;

	@Autowired
	private PropertyService _propertyService;

}