/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccount;
import com.liferay.one.exception.LicenseKeyActiveException;
import com.liferay.one.exception.LicenseKeyValidationException;
import com.liferay.one.exception.NoSuchLicenseKeyException;
import com.liferay.one.jira.exception.AccountNotFoundException;
import com.liferay.one.jira.exception.JiraAssetObjectException;
import com.liferay.one.service.UserAccountService;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.security.Principal;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Amos Fong
 */
public abstract class OneBaseRestController extends BaseRestController {

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<?> handleException(
		AccountNotFoundException accountNotFoundException) {

		_log.error("The account was not found", accountNotFoundException);

		return _toResponseEntity(
			HttpStatus.NOT_FOUND, "The account was not found");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleException(Exception exception) {
		_log.error("An unexpected error occurred", exception);

		return _toResponseEntity(
			HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	@ExceptionHandler(JiraAssetObjectException.class)
	public ResponseEntity<?> handleException(
		JiraAssetObjectException jiraAssetObjectException) {

		_log.error("The asset object was not found", jiraAssetObjectException);

		return _toResponseEntity(
			HttpStatus.NOT_FOUND, jiraAssetObjectException.getMessage());
	}

	@ExceptionHandler(LicenseKeyActiveException.class)
	public ResponseEntity<?> handleException(
		LicenseKeyActiveException licenseKeyActiveException) {

		_log.error("The license key is inactive", licenseKeyActiveException);

		return _toResponseEntity(
			HttpStatus.CONFLICT, "The license key is inactive");
	}

	@ExceptionHandler(LicenseKeyValidationException.class)
	public ResponseEntity<?> handleException(
		LicenseKeyValidationException licenseKeyValidationException) {

		_log.error(
			"The license key is not valid", licenseKeyValidationException);

		return _toResponseEntity(
			HttpStatus.BAD_REQUEST, licenseKeyValidationException.getMessage());
	}

	@ExceptionHandler(NoSuchLicenseKeyException.class)
	public ResponseEntity<?> handleException(
		NoSuchLicenseKeyException noSuchLicenseKeyException) {

		_log.error("The license key was not found", noSuchLicenseKeyException);

		return _toResponseEntity(
			HttpStatus.NOT_FOUND, "The license key was not found");
	}

	@ExceptionHandler(PrincipalException.class)
	public ResponseEntity<?> handleException(
		Principal principal, PrincipalException principalException) {

		JwtAuthenticationToken jwtAuthenticationToken =
			(JwtAuthenticationToken)principal;

		Jwt jwt = jwtAuthenticationToken.getToken();

		_log.error(
			"Permission denied for " + jwt.getSubject(), principalException);

		return _toResponseEntity(
			HttpStatus.FORBIDDEN,
			"You do not have permission to access this resource");
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<?> handleException(
		ResponseStatusException responseStatusException) {

		_log.error(responseStatusException.getBody(), responseStatusException);

		return new ResponseEntity<>(
			responseStatusException.getBody(),
			responseStatusException.getStatusCode());
	}

	protected UserAccount getMyUserAccount(Jwt jwt) throws Exception {
		try {
			return _userAccountService.getMyUserAccount(jwt);
		}
		catch (Exception exception) {
			if (_log.isWarnEnabled()) {
				_log.warn("Unable to get user account", exception);
			}

			throw new PrincipalException();
		}
	}

	protected <T> ResponseEntity<String> getResponseEntity(
		List<T> items, Function<T, JSONObject> transformFunction) {

		JSONObject responseJSONObject = new JSONObject();

		JSONArray itemsJSONArray = new JSONArray();

		for (T item : items) {
			itemsJSONArray.put(transformFunction.apply(item));
		}

		responseJSONObject.put("items", itemsJSONArray);

		return new ResponseEntity<>(
			responseJSONObject.toString(), HttpStatus.OK);
	}

	private ResponseEntity<ProblemDetail> _toResponseEntity(
		HttpStatus httpStatus, String detail) {

		return new ResponseEntity<>(
			ProblemDetail.forStatusAndDetail(httpStatus, detail), httpStatus);
	}

	private static final Log _log = LogFactory.getLog(
		OneBaseRestController.class);

	@Autowired
	private UserAccountService _userAccountService;

}