/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.okta.model.OktaUser;
import com.liferay.one.okta.service.OktaService;
import com.liferay.one.service.EmailAddressValidatorService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Allen Ziegenfus
 */
@RequestMapping("/contacts")
@RestController
public class ContactsRestController extends OneBaseRestController {

	@GetMapping("/{contactEmailAddress}/validate")
	public boolean getContactsValidate(
		@PathVariable("contactEmailAddress") String contactEmailAddress) {

		if (!_emailAddressValidatorService.isLiferayDomain(
				contactEmailAddress)) {

			return true;
		}

		try {
			OktaUser oktaUser = _oktaService.fetchContactByEmailAddress(
				contactEmailAddress);

			if (oktaUser != null) {
				return true;
			}

			return false;
		}
		catch (Exception exception) {
			_log.error(
				"Unable to look up the Okta contact for " + contactEmailAddress,
				exception);

			throw new ResponseStatusException(
				HttpStatus.SERVICE_UNAVAILABLE,
				"Unable to verify the Liferay contact for " +
					contactEmailAddress);
		}
	}

	private static final Log _log = LogFactory.getLog(
		ContactsRestController.class);

	@Autowired
	private EmailAddressValidatorService _emailAddressValidatorService;

	@Autowired
	private OktaService _oktaService;

}