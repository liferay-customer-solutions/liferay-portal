/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Allen Ziegenfus
 */
public class TermCountUtilTest {

	@Test
	public void testConsolidateWhenDatesAreNull() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		TermCountUtil.consolidate(termedCounts, _CURRENT_YEAR, null, null, 2);

		Map<Integer, Integer> maxConcurrentCounts =
			TermCountUtil.getMaxConcurrentCounts(termedCounts, _CURRENT_YEAR);

		Assertions.assertEquals(2, maxConcurrentCounts.get(_CURRENT_YEAR - 1));

		Assertions.assertEquals(2, maxConcurrentCounts.get(_CURRENT_YEAR));
	}

	@Test
	public void testConsolidateWhenTermEndedBeforeTheReportedYears() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR,
			TermCountUtil.getStartOfYearInstant(_CURRENT_YEAR - 6),
			TermCountUtil.getStartOfYearInstant(_CURRENT_YEAR - 5), 3);

		Assertions.assertTrue(termedCounts.isEmpty());

		Assertions.assertTrue(
			TermCountUtil.getMaxConcurrentCounts(
				termedCounts, _CURRENT_YEAR
			).isEmpty());
	}

	@Test
	public void testConsolidateWhenTermsDoNotOverlap() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		Instant startOfYearInstant = TermCountUtil.getStartOfYearInstant(
			_CURRENT_YEAR);

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR, startOfYearInstant,
			startOfYearInstant.plus(90, ChronoUnit.DAYS), 1);

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR,
			startOfYearInstant.plus(180, ChronoUnit.DAYS),
			startOfYearInstant.plus(270, ChronoUnit.DAYS), 1);

		Map<Integer, Integer> maxConcurrentCounts =
			TermCountUtil.getMaxConcurrentCounts(termedCounts, _CURRENT_YEAR);

		Assertions.assertEquals(1, maxConcurrentCounts.get(_CURRENT_YEAR));
	}

	@Test
	public void testConsolidateWhenTermsOverlap() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		Instant startOfYearInstant = TermCountUtil.getStartOfYearInstant(
			_CURRENT_YEAR);

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR, startOfYearInstant,
			startOfYearInstant.plus(180, ChronoUnit.DAYS), 1);

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR,
			startOfYearInstant.plus(90, ChronoUnit.DAYS),
			startOfYearInstant.plus(270, ChronoUnit.DAYS), 1);

		Map<Integer, Integer> maxConcurrentCounts =
			TermCountUtil.getMaxConcurrentCounts(termedCounts, _CURRENT_YEAR);

		Assertions.assertEquals(2, maxConcurrentCounts.get(_CURRENT_YEAR));
	}

	@Test
	public void testGetCurrentCount() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		Assertions.assertEquals(0, TermCountUtil.getCurrentCount(termedCounts));

		Instant instant = Instant.now();

		TermCountUtil.consolidate(
			termedCounts, TermCountUtil.getYear(instant),
			instant.minus(30, ChronoUnit.DAYS),
			instant.plus(30, ChronoUnit.DAYS), 4);

		Assertions.assertEquals(4, TermCountUtil.getCurrentCount(termedCounts));
	}

	@Test
	public void testGetMaxConcurrentCountsIsCappedAtTheNextYear() {
		TreeMap<Instant, Integer> termedCounts = new TreeMap<>();

		TermCountUtil.consolidate(
			termedCounts, _CURRENT_YEAR,
			TermCountUtil.getStartOfYearInstant(_CURRENT_YEAR),
			TermCountUtil.getStartOfYearInstant(_CURRENT_YEAR + 10), 7);

		Map<Integer, Integer> maxConcurrentCounts =
			TermCountUtil.getMaxConcurrentCounts(termedCounts, _CURRENT_YEAR);

		Assertions.assertEquals(7, maxConcurrentCounts.get(_CURRENT_YEAR));

		Assertions.assertEquals(7, maxConcurrentCounts.get(_CURRENT_YEAR + 1));

		Assertions.assertNull(maxConcurrentCounts.get(_CURRENT_YEAR + 2));
	}

	@Test
	public void testGetStartOfYearInstant() {
		Assertions.assertEquals(
			"2026-01-01T00:00:00Z",
			String.valueOf(TermCountUtil.getStartOfYearInstant(2026)));
	}

	@Test
	public void testGetYear() {
		Assertions.assertEquals(
			2026, TermCountUtil.getYear(Instant.parse("2026-06-15T12:00:00Z")));
	}

	private static final int _CURRENT_YEAR = 2026;

}