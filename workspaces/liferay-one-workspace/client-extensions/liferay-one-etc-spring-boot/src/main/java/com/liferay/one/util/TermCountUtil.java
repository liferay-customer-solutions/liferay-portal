/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author Allen Ziegenfus
 */
public class TermCountUtil {

	public static void consolidate(
		TreeMap<Instant, Integer> termedCounts, int currentYear,
		Instant startInstant, Instant endInstant, int count) {

		if (startInstant == null) {
			startInstant = getStartOfYearInstant(currentYear - 1);
		}

		if (endInstant == null) {
			endInstant = getStartOfYearInstant(currentYear + 1);
		}

		if (endInstant.isBefore(getStartOfYearInstant(currentYear - 1))) {
			return;
		}

		Map.Entry<Instant, Integer> previousEntry = termedCounts.floorEntry(
			startInstant);
		Map.Entry<Instant, Integer> nextEntry = termedCounts.higherEntry(
			startInstant);

		int previousCount = 0;

		if (previousEntry != null) {
			previousCount = previousEntry.getValue();
		}

		if (nextEntry == null) {
			termedCounts.put(endInstant, previousCount);
			termedCounts.put(startInstant, count + previousCount);

			return;
		}

		termedCounts.put(startInstant, count + previousCount);

		Instant nextInstant = nextEntry.getKey();

		while (nextInstant.isBefore(endInstant)) {
			termedCounts.put(nextInstant, nextEntry.getValue() + count);

			nextEntry = termedCounts.higherEntry(nextInstant);

			if (nextEntry == null) {
				termedCounts.put(endInstant, 0);

				return;
			}

			nextInstant = nextEntry.getKey();
		}

		if (!nextInstant.equals(endInstant)) {
			previousEntry = termedCounts.floorEntry(endInstant);

			termedCounts.put(endInstant, previousEntry.getValue() - count);
		}
	}

	public static int getCurrentCount(TreeMap<Instant, Integer> termedCounts) {
		Map.Entry<Instant, Integer> entry = termedCounts.floorEntry(
			Instant.now());

		if (entry == null) {
			return 0;
		}

		return entry.getValue();
	}

	public static Map<Integer, Integer> getMaxConcurrentCounts(
		TreeMap<Instant, Integer> termedCounts, int currentYear) {

		if (termedCounts.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Integer, Integer> maxConcurrentCounts = new HashMap<>();

		Map.Entry<Instant, Integer> previousEntry = null;

		for (Map.Entry<Instant, Integer> entry : termedCounts.entrySet()) {
			if (previousEntry != null) {
				_putMaxConcurrentCounts(
					currentYear, getYear(entry.getKey()), maxConcurrentCounts,
					previousEntry);
			}

			previousEntry = entry;
		}

		_putMaxConcurrentCounts(
			currentYear, currentYear + 1, maxConcurrentCounts, previousEntry);

		return maxConcurrentCounts;
	}

	public static Instant getStartOfYearInstant(int year) {
		LocalDate localDate = LocalDate.of(year, 1, 1);

		return localDate.atStartOfDay(
			ZoneOffset.UTC
		).toInstant();
	}

	public static int getYear(Instant instant) {
		LocalDate localDate = LocalDate.ofInstant(instant, ZoneOffset.UTC);

		return localDate.getYear();
	}

	private static void _putMaxConcurrentCounts(
		int currentYear, int endYear, Map<Integer, Integer> maxConcurrentCounts,
		Map.Entry<Instant, Integer> previousEntry) {

		if (endYear > (currentYear + 1)) {
			endYear = currentYear + 1;
		}

		int year = getYear(previousEntry.getKey());

		while (year <= endYear) {
			Integer maxConcurrentCount = maxConcurrentCounts.get(year);

			if ((maxConcurrentCount == null) ||
				(maxConcurrentCount < previousEntry.getValue())) {

				maxConcurrentCount = previousEntry.getValue();
			}

			maxConcurrentCounts.put(year, maxConcurrentCount);

			year++;
		}
	}

}