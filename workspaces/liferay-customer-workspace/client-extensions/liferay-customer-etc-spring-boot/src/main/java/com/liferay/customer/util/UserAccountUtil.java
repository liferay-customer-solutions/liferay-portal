/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.customer.util;

import com.liferay.headless.admin.user.client.dto.v1_0.Phone;
import com.liferay.headless.admin.user.client.dto.v1_0.UserAccountContactInformation;
import com.liferay.portal.kernel.util.ArrayUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Felipe Franca
 */
public class UserAccountUtil {

	public static UserAccountContactInformation
		updateUserAccountContactInformation(
			com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Phone[]
				contactPhones,
			UserAccountContactInformation userAccountContactInformation) {

		List<Phone> newPhones = new ArrayList<>();

		if (userAccountContactInformation == null) {
			userAccountContactInformation = new UserAccountContactInformation();

			userAccountContactInformation.setTelephones(
				() -> newPhones.toArray(new Phone[0]));
		}

		if (ArrayUtil.isEmpty(contactPhones)) {
			userAccountContactInformation.setTelephones(
				() -> newPhones.toArray(new Phone[0]));

			return userAccountContactInformation;
		}

		for (com.liferay.osb.koroneiki.phloem.rest.client.dto.v1_0.Phone phone :
				contactPhones) {

			String type = phone.getTypeAsString();
			Boolean primary = phone.getPrimary();

			boolean updated = false;

			for (Phone curPhone :
					userAccountContactInformation.getTelephones()) {

				String curType = curPhone.getPhoneType();
				Boolean curPrimary = curPhone.getPrimary();

				if ((type.equals("Mobile") && curType.equals("mobile-phone")) ||
					(type.equals("Other") && curType.equals("other") &&
					 primary && curPrimary)) {

					curPhone.setPhoneNumber(phone::getNumber);

					newPhones.add(curPhone);

					updated = true;

					break;
				}
			}

			if (!updated) {
				Phone newPhone = new Phone();

				newPhone.setPhoneNumber(phone::getNumber);

				if (type.equals("Mobile")) {
					newPhone.setPhoneType(() -> "mobile-phone");
				}
				else {
					newPhone.setPhoneType(() -> "other");
				}

				newPhone.setPrimary(() -> primary);

				newPhones.add(newPhone);
			}
		}

		userAccountContactInformation.setTelephones(
			() -> newPhones.toArray(new Phone[0]));

		return userAccountContactInformation;
	}

}