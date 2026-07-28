/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {useEffect, useState} from 'react';
import LicenseKeysService from '~/services/spring-boot/LicenseKeys';

export function useLicenseKeySubscription(licenseKeyId: string) {
	const [loading, setLoading] = useState(true);
	const [subscribed, setSubscribed] = useState(false);

	useEffect(() => {
		let active = true;

		setLoading(true);

		LicenseKeysService.getSubscription(licenseKeyId)
			.then((value) => {
				if (active) {
					setSubscribed(value);
				}
			})
			.catch(() => {
				if (active) {
					setSubscribed(false);
				}
			})
			.finally(() => {
				if (active) {
					setLoading(false);
				}
			});

		return () => {
			active = false;
		};
	}, [licenseKeyId]);

	async function toggleSubscription(nextSubscribed: boolean) {
		setSubscribed(nextSubscribed);

		try {
			if (nextSubscribed) {
				await LicenseKeysService.subscribe(licenseKeyId);
			}
			else {
				await LicenseKeysService.unsubscribe(licenseKeyId);
			}
		}
		catch (error) {
			setSubscribed(!nextSubscribed);
		}
	}

	return {loading, subscribed, toggleSubscription};
}

export default useLicenseKeySubscription;
