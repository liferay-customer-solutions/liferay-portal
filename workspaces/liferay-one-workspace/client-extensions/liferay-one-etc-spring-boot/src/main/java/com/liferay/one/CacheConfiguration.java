/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Jenny Chen
 */
@Configuration
public class CacheConfiguration {

	@Bean
	public CacheManager cacheManager() {
		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
			"assetObjectFieldOptions", "assetObjectTypeAttributeIds",
			"assetObjectTypeAttributeOptions", "assetObjectTypeIds",
			"productVersions");

		caffeineCacheManager.setCaffeine(
			Caffeine.newBuilder(
			).maximumSize(
				1000
			));

		caffeineCacheManager.registerCustomCache(
			"composableAccountUsage",
			Caffeine.newBuilder(
			).expireAfterWrite(
				Duration.ofHours(1)
			).maximumSize(
				1000
			).build());

		caffeineCacheManager.registerCustomCache(
			"customerAccountUsage",
			Caffeine.newBuilder(
			).expireAfterWrite(
				Duration.ofHours(1)
			).maximumSize(
				1000
			).build());

		caffeineCacheManager.registerCustomCache(
			"productName",
			Caffeine.newBuilder(
			).expireAfterWrite(
				Duration.ofHours(1)
			).maximumSize(
				1000
			).build());

		return caffeineCacheManager;
	}

}