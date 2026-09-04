/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.liferay.one.jira.synchronizer.TeamRoleSynchronizer;
import com.liferay.one.permission.AdminPermission;
import com.liferay.one.pubsub.Message;
import com.liferay.one.pubsub.subscriber.BasePubsubSubscriber;
import com.liferay.one.service.LDPEventUsageReportService;
import com.liferay.petra.string.CharPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ryan Schuhler
 * @author Kyle Bischof
 */
@RequestMapping("/admin")
@RestController
public class AdminRestController extends OneBaseRestController {

	@GetMapping("/pubsub/subscribers")
	public ResponseEntity<String> getPubsubSubscribers(
			@AuthenticationPrincipal Jwt jwt)
		throws Exception {

		_adminPermission.check(jwt);

		JSONArray jsonArray = new JSONArray();

		for (BasePubsubSubscriber basePubsubSubscriber :
				_basePubsubSubscribers) {

			String topic = basePubsubSubscriber.getTopic();

			if (Validator.isNull(topic)) {
				continue;
			}

			Class<?> clazz = basePubsubSubscriber.getClass();

			jsonArray.put(
				new JSONObject(
				).put(
					"name", clazz.getSimpleName()
				).put(
					"topic", topic
				));
		}

		return new ResponseEntity<>(jsonArray.toString(), HttpStatus.OK);
	}

	@PostMapping("/jira/team-roles/sync")
	public ResponseEntity<String> postJiraTeamRolesSync(
			@AuthenticationPrincipal Jwt jwt)
		throws Exception {

		_adminPermission.check(jwt);

		_teamRoleSynchronizer.syncTeamRoles();

		JSONObject jsonObject = new JSONObject(
		).put(
			"firstLineSupportTeamRoleObjectId",
			_teamRoleSynchronizer.getFirstLineSupportTeamRoleObjectId()
		);

		return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
	}

	@PostMapping("/ldp/event-usage-reports/generate")
	public ResponseEntity<String> postLDPEventUsageReportsGenerate(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String yearMonth)
		throws Exception {

		_adminPermission.check(jwt);

		YearMonth reportYearMonth = null;

		try {
			reportYearMonth = _getYearMonth(yearMonth);
		}
		catch (DateTimeParseException dateTimeParseException) {
			throw new ResponseStatusException(
				HttpStatus.BAD_REQUEST,
				"Year month must use the format yyyy-MM: " + yearMonth,
				dateTimeParseException);
		}

		_ldpEventUsageReportService.generateUsageReports(reportYearMonth);

		JSONObject jsonObject = new JSONObject(
		).put(
			"yearMonth", String.valueOf(reportYearMonth)
		);

		return new ResponseEntity<>(jsonObject.toString(), HttpStatus.OK);
	}

	@PostMapping("/pubsub/dispatch")
	public ResponseEntity<Void> postPubsubDispatch(
			@AuthenticationPrincipal Jwt jwt, @RequestBody String json)
		throws Exception {

		_adminPermission.check(jwt);

		JSONObject jsonObject = new JSONObject(json);

		Map<String, String> attributes = _parseAttributes(
			jsonObject.optString("attributes"));
		String payload = StringUtil.removeChars(
			jsonObject.optString("payload"), CharPool.NEW_LINE,
			CharPool.RETURN);
		String topic = jsonObject.optString("topic");

		boolean dispatched = false;

		for (BasePubsubSubscriber basePubsubSubscriber :
				_basePubsubSubscribers) {

			if (!topic.equals(basePubsubSubscriber.getTopic())) {
				continue;
			}

			try {
				basePubsubSubscriber.receive(
					new Message(attributes, payload, topic));

				dispatched = true;
			}
			catch (Exception exception) {
				Class<?> clazz = basePubsubSubscriber.getClass();

				throw new ResponseStatusException(
					HttpStatus.BAD_GATEWAY,
					"Unable to dispatch message to Pubsub subscriber " +
						clazz.getSimpleName(),
					exception);
			}
		}

		if (!dispatched) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"Unable o find Pubsub subscriber for topic " + topic);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private YearMonth _getYearMonth(String yearMonthString)
		throws DateTimeParseException {

		if (Validator.isNull(yearMonthString)) {
			return YearMonth.now(
				ZoneOffset.UTC
			).minusMonths(
				1
			);
		}

		return YearMonth.parse(yearMonthString);
	}

	private Map<String, String> _parseAttributes(String attributes) {
		Map<String, String> attributesMap = new LinkedHashMap<>();

		String[] lines = StringUtil.split(attributes, CharPool.NEW_LINE);

		for (String line : lines) {
			int index = line.indexOf(CharPool.EQUAL);

			if (index <= 0) {
				throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Unable to parse attributes line: " + line);
			}

			attributesMap.put(
				line.substring(0, index), line.substring(index + 1));
		}

		return attributesMap;
	}

	@Autowired
	private AdminPermission _adminPermission;

	@Autowired(required = false)
	private List<BasePubsubSubscriber> _basePubsubSubscribers =
		Collections.emptyList();

	@Autowired
	private LDPEventUsageReportService _ldpEventUsageReportService;

	@Autowired
	private TeamRoleSynchronizer _teamRoleSynchronizer;

}