/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.client.LiferayOAuth2AccessTokenManager;
import com.liferay.petra.function.UnsafeRunnable;
import com.liferay.petra.string.StringBundler;

import java.time.ZonedDateTime;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Keven Leone
 * @author Wellington Barbosa
 */
@Component
public class TrialCommandLineRunner
	extends BaseRestController implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		_invoke(this::_processInProgressTrials, "In Progress Trials");

		_invoke(this::_processOnHoldTrials, "On Hold Trials");
	}

	private JSONObject _getAvailabilityJSONObject() throws Exception {
		return new JSONObject(
			get(
				_liferayOAuth2AccessTokenManager.getAuthorization(
					_liferayOAuthApplicationExternalReferenceCodes),
				UriComponentsBuilder.fromUriString(
					_liferayOneEtcSpringBootURL + "/trial/availability"
				).build(
				).toUri()));
	}

	private JSONArray _getOrdersJSONArray(String filterString)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			get(
				_liferayOAuth2AccessTokenManager.getAuthorization(
					_liferayOAuthApplicationExternalReferenceCodes),
				UriComponentsBuilder.fromPath(
					"/o/headless-commerce-admin-order/v1.0/orders"
				).queryParam(
					"filter", filterString
				).queryParam(
					"nestedFields", "customFields"
				).queryParam(
					"page", "-1"
				).queryParam(
					"pageSize", "-1"
				).build(
				).encode(
				).toUri()));

		return jsonObject.optJSONArray("items");
	}

	private void _invoke(UnsafeRunnable<?> task, String name) {
		if (_log.isInfoEnabled()) {
			_log.info("Processing \"" + name + "\"");
		}

		try {
			task.run();
		}
		catch (Throwable throwable) {
			_log.error("Unable to process " + name, throwable);
		}
	}

	private void _postTrialExpire(long orderId) throws Exception {
		post(
			_liferayOAuth2AccessTokenManager.getAuthorization(
				_liferayOAuthApplicationExternalReferenceCodes),
			"",
			UriComponentsBuilder.fromUriString(
				_liferayOneEtcSpringBootURL + "/trial/expire/" + orderId
			).build(
			).toUri());
	}

	private void _postTrialNotifyEnd(long orderId) throws Exception {
		post(
			_liferayOAuth2AccessTokenManager.getAuthorization(
				_liferayOAuthApplicationExternalReferenceCodes),
			"",
			UriComponentsBuilder.fromUriString(
				_liferayOneEtcSpringBootURL + "/trial/notify-end/" + orderId
			).build(
			).toUri());
	}

	private void _postTrialProvisioning(long orderId) throws Exception {
		post(
			_liferayOAuth2AccessTokenManager.getAuthorization(
				_liferayOAuthApplicationExternalReferenceCodes),
			new JSONObject(
			).put(
				"classPK", orderId
			).toString(),
			UriComponentsBuilder.fromUriString(
				_liferayOneEtcSpringBootURL + "/trial/provisioning"
			).build(
			).toUri());
	}

	private void _processInProgressTrials() throws Exception {
		JSONArray ordersJSONArray = _getOrdersJSONArray(
			StringBundler.concat(
				"orderStatus/any(x:(x eq ", _ORDER_STATUS_IN_PROGRESS,
				")) and orderTypeExternalReferenceCode in (",
				"'SSA_SAAS', 'SOLUTIONS7')"));

		if (ordersJSONArray == null) {
			return;
		}

		for (int i = 0; i < ordersJSONArray.length(); i++) {
			JSONObject orderJSONObject = ordersJSONArray.getJSONObject(i);

			try {
				long orderId = orderJSONObject.getLong("id");

				JSONObject customFieldsJSONObject =
					orderJSONObject.optJSONObject("customFields");

				if (customFieldsJSONObject == null) {
					continue;
				}

				String trialEndDate = customFieldsJSONObject.optString(
					"trial-end-date");

				if (trialEndDate.isEmpty()) {
					continue;
				}

				ZonedDateTime nowZonedDateTime = ZonedDateTime.now();

				ZonedDateTime trialEndDateZonedDateTime = ZonedDateTime.parse(
					trialEndDate);

				if (nowZonedDateTime.isAfter(trialEndDateZonedDateTime)) {
					_postTrialExpire(orderId);

					if (_log.isInfoEnabled()) {
						_log.info("Processed expired order " + orderId);
					}

					continue;
				}

				String trialNotifyEndDate = customFieldsJSONObject.optString(
					"trial-notify-end-date");

				if (trialNotifyEndDate.isEmpty() &&
					Objects.equals(
						nowZonedDateTime.withZoneSameInstant(
							trialEndDateZonedDateTime.getZone()
						).toLocalDate(),
						trialEndDateZonedDateTime.minusDays(
							1
						).toLocalDate())) {

					_postTrialNotifyEnd(orderId);

					if (_log.isInfoEnabled()) {
						_log.info(
							"Processed notify end of trial for order " +
								orderId);
					}
				}
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}
	}

	private void _processOnHoldTrials() throws Exception {
		JSONArray ordersJSONArray = _getOrdersJSONArray(
			StringBundler.concat(
				"orderStatus/any(x:(x eq ", _ORDER_STATUS_ON_HOLD,
				")) and orderTypeExternalReferenceCode eq 'SOLUTIONS7'"));

		if ((ordersJSONArray == null) || (ordersJSONArray.length() == 0)) {
			return;
		}

		JSONObject availabilityJSONObject = _getAvailabilityJSONObject();

		if (!availabilityJSONObject.getBoolean("active")) {
			if (_log.isInfoEnabled()) {
				_log.info("There are no available seats");
			}

			return;
		}

		long available = availabilityJSONObject.getLong("available");

		for (int i = 0; i < ordersJSONArray.length(); i++) {
			if (available <= 0) {
				if (_log.isInfoEnabled()) {
					_log.info("There are no available seats");
				}

				break;
			}

			JSONObject orderJSONObject = ordersJSONArray.getJSONObject(i);

			try {
				long orderId = orderJSONObject.getLong("id");

				if (_log.isInfoEnabled()) {
					_log.info("Processing on hold order " + orderId);
				}

				_postTrialProvisioning(orderId);

				if (_log.isInfoEnabled()) {
					_log.info("Processed on hold order " + orderId);
				}

				available--;
			}
			catch (Exception exception) {
				_log.error(exception);
			}
		}
	}

	private static final int _ORDER_STATUS_IN_PROGRESS = 6;

	private static final int _ORDER_STATUS_ON_HOLD = 20;

	private static final Log _log = LogFactory.getLog(
		TrialCommandLineRunner.class);

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${liferay.oauth.application.external.reference.codes}")
	private String _liferayOAuthApplicationExternalReferenceCodes;

	@Value("${liferay.one.etc.spring.boot.url}")
	private String _liferayOneEtcSpringBootURL;

}