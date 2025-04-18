/**
 * SPDX-FileCopyrightText: (c) 2023 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer;

import com.liferay.client.extension.util.spring.boot3.BaseRestController;
import com.liferay.client.extension.util.spring.boot3.LiferayOAuth2AccessTokenManager;
import com.liferay.osb.spring.boot.client.zendesk.model.ZendeskTicket;
import com.liferay.osb.spring.boot.client.zendesk.search.SearchHits;
import com.liferay.osb.spring.boot.client.zendesk.search.ZendeskTicketQuery;
import com.liferay.osb.spring.boot.client.zendesk.service.ZendeskService;
import com.liferay.petra.string.StringBundler;

import java.text.SimpleDateFormat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.Date;

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
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * @author Amos Fong
 */
@Component
@ComponentScan(basePackages = "com.liferay.osb")
public class CustomerCommandLineRunner
	extends BaseRestController implements CommandLineRunner {

	@Override
	public void run(String... args) throws Exception {
		if (_log.isInfoEnabled()) {
			_log.info("Starting CustomerCommandLineRunner");
		}

		_runAtInterval(
			"weekly",
			() -> {
				try {
					_cleanTicketAttachments();
				}
				catch (Exception exception) {
					_log.error("Error cleaning ticket attachments", exception);
				}
			});

		if (_log.isInfoEnabled()) {
			_log.info("Finished running all tasks. Shutting down application.");
		}

		ConfigurableApplicationContext configurableApplicationContext =
			(ConfigurableApplicationContext)_applicationContext;

		configurableApplicationContext.close();
	}

	private void _cleanTicketAttachments() throws Exception {
		ZendeskTicketQuery zendeskTicketQuery = new ZendeskTicketQuery();

		zendeskTicketQuery.addCriterion("status:closed");

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		Date startDate = new Date(
			System.currentTimeMillis() -
				((_zendeskTicketClosedDays + 7) * 24 * 60 * 60 * 1000));

		zendeskTicketQuery.addCriterion(
			"updated>" + simpleDateFormat.format(startDate));

		Date endDate = new Date(
			System.currentTimeMillis() -
				(_zendeskTicketClosedDays * 24 * 60 * 60 * 1000));

		zendeskTicketQuery.addCriterion(
			"updated<" + simpleDateFormat.format(endDate));

		int page = 1;

		while (page > 0) {
			zendeskTicketQuery.setPage(page);

			SearchHits<ZendeskTicket> searchHits = _zendeskService.search(
				zendeskTicketQuery);

			for (ZendeskTicket zendeskTicket : searchHits.getResults()) {
				_deleteTicketAttachments(zendeskTicket.getZendeskTicketId());
			}

			page = searchHits.getNextPage();
		}
	}

	private void _deleteTicketAttachments(long zendeskTicketId)
		throws Exception {

		JSONObject jsonObject = new JSONObject(
			get(
				_getAuthorization(),
				_defaultUriBuilderFactory.builder(
				).path(
					"/o/c/ticketattachments"
				).queryParam(
					"filter=zendeskTicketId eq " + zendeskTicketId
				).build(
				).toString()));

		JSONArray jsonArray = jsonObject.getJSONArray("items");

		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject ticketAttachmentJSONObject = jsonArray.getJSONObject(i);

			if (_log.isDebugEnabled()) {
				_log.debug(
					"Deleting ticket attachment " +
						ticketAttachmentJSONObject.getString("id"));
			}

			delete(
				_getAuthorization(), "",
				"/ticket-attachments/" +
					ticketAttachmentJSONObject.getInt("id"));
		}
	}

	private String _getAuthorization() {
		return _liferayOAuth2AccessTokenManager.getAuthorization(
			"liferay-customer-etc-cron-oahs");
	}

	private void _runAtInterval(String interval, Runnable task) {
		if (!interval.equals("quarter-hourly") && !interval.equals("hourly") &&
			!interval.equals("daily") && !interval.equals("weekly") &&
			!interval.equals("monthly")) {

			_log.error(
				"Invalid interval. Expected values are 'quarter-hourly', " +
					"'hourly', 'daily', 'weekly', or 'monthly'.");

			return;
		}

		ZonedDateTime zonedDateTime = Instant.now(
		).atZone(
			ZoneOffset.UTC
		);

		int currentDayOfMonth = zonedDateTime.getDayOfMonth();
		DayOfWeek currentDayOfWeek = zonedDateTime.getDayOfWeek();
		int currentHour = zonedDateTime.getHour();
		int currentMinute = zonedDateTime.getMinute();

		boolean shouldRun = false;

		if (interval.equals("quarter-hourly")) {
			shouldRun = (currentMinute % 15) == 0;
		}
		else if (interval.equals("hourly")) {
			shouldRun = currentMinute == 0;
		}
		else if (interval.equals("daily")) {
			shouldRun = (currentHour == 1) && (currentMinute == 0);
		}
		else if (interval.equals("weekly")) {
			shouldRun =
				(currentDayOfWeek == DayOfWeek.SUNDAY) && (currentHour == 1) &&
				(currentMinute == 0);
		}
		else if (interval.equals("monthly")) {
			shouldRun =
				(currentDayOfMonth == 1) && (currentHour == 1) &&
				(currentMinute == 0);
		}

		if (shouldRun) {
			if (_log.isInfoEnabled()) {
				_log.info("It is time to run the " + interval + " tasks.");
			}

			try {
				_taskExecutor.execute(task);
			}
			catch (Exception exception) {
				_log.error("Error running " + interval + " tasks", exception);
			}
		}
		else {
			if (_log.isInfoEnabled()) {
				_log.info(
					StringBundler.concat(
						"Not the time to run the ", interval,
						" tasks. Current time is: ", currentHour, ":",
						currentMinute, " and day is: ", currentDayOfWeek,
						" and day of month is: ", currentDayOfMonth));
			}
		}
	}

	private static final Log _log = LogFactory.getLog(
		CustomerCommandLineRunner.class);

	@Autowired
	private ApplicationContext _applicationContext;

	private final DefaultUriBuilderFactory _defaultUriBuilderFactory =
		new DefaultUriBuilderFactory();

	@Value("${liferay.customer.etc.spring.boot.client.extension.url}")
	private String _etcSpringBootClientExtensionURL;

	@Autowired
	private LiferayOAuth2AccessTokenManager _liferayOAuth2AccessTokenManager;

	@Value("${com.liferay.lxc.dxp.mainDomain}")
	private String _lxcDXPMainDomain;

	@Value("${com.liferay.lxc.dxp.server.protocol}")
	private String _lxcDXPServerProtocol;

	@Value("${liferay.customer.releases.url}")
	private String _releasesURL;

	@Autowired
	private TaskExecutor _taskExecutor;

	@Autowired
	private ZendeskService _zendeskService;

	@Value("${liferay.customer.zendesk.ticket.closed.days}")
	private int _zendeskTicketClosedDays;

}