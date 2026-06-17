/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one;

import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
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
	public CacheManager cacheManager(
		@Value("${liferay.one.okta.oauth.token.cache.ttl.seconds:300}") long
			oauthTokenCacheTtlSeconds) {

		CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager(
			"assetObjectFieldOptions", "assetObjects");

		caffeineCacheManager.setCaffeine(
			Caffeine.newBuilder(
			).maximumSize(
				1000
			));

		caffeineCacheManager.registerCustomCache(
			"oauthTokenUsers",
			Caffeine.newBuilder(
			).expireAfterWrite(
				oauthTokenCacheTtlSeconds, TimeUnit.SECONDS
			).maximumSize(
				1000
			).build());

		return caffeineCacheManager;
	}

}