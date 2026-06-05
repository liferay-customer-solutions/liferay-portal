/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.one.service;

import com.liferay.one.model.LicenseKey;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * @author Amos Fong
 */
@Component
public class LicenseKeyService extends OneBaseService {

	public List<LicenseKey> getLicenseKeys(String filterString)
		throws Exception {

		return getAllItems("/o/c/licensekeys", filterString, LicenseKey::new);
	}

}