/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {OneSpringBootOAuth2} from './OAuth2Client';

export type Subscriber = {
	name: string;
	topic: string;
};

class DispatchMessageOAuth2 extends OneSpringBootOAuth2 {
	async dispatchMessage(message: {
		attributes: string;
		payload: string;
		topic: string;
	}): Promise<void> {
		await this.post('/dispatch', message, {
			headers: {'Content-Type': 'application/json'},
		});
	}

	async getTopics(): Promise<Subscriber[]> {
		return this.get('/subscribers');
	}
}

const DispatchMessage = new DispatchMessageOAuth2('/admin/pubsub');

export default DispatchMessage;
