/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.portal.kernel.security.auth.PrincipalException;

import java.security.Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleException(Exception exception) {
		_log.error("An unexpected error occurred", exception);

		return _toResponseEntity(
			HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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

	private ResponseEntity<ProblemDetail> _toResponseEntity(
		HttpStatus httpStatus, String detail) {

		return new ResponseEntity<>(
			ProblemDetail.forStatusAndDetail(httpStatus, detail), httpStatus);
	}

	private static final Log _log = LogFactory.getLog(
		OneBaseRestController.class);

}