/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

const productIcon = fragmentElement.querySelector('#product-icon');
const productIconImg = fragmentElement.querySelector('#product-icon-img');

productIconImg.src = productIcon.textContent.trim();
