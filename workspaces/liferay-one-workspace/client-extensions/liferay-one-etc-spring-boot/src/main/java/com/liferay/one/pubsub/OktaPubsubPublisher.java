/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.pubsub;

import com.liferay.one.pubsub.publisher.BasePubsubPublisher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Karoline Silva
 */
@Component
public class OktaPubsubPublisher extends BasePubsubPublisher {

	@Override
	protected String getProjectId() {
		return _projectId;
	}

	@Override
	protected boolean isAutoCreateTopic() {
		return false;
	}

	@Value("${liferay.one.okta.pubsub.publisher.project.id}")
	private String _projectId;

}