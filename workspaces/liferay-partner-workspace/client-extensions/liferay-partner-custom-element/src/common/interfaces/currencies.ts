/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import LiferayItems from "../services/liferay/common/interfaces/liferayItems";
import Currency from "./currency";

export default interface Currencies extends Partial<LiferayItems<Currency[]>>  {
        items: Currency[];
}
