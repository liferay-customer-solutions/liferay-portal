/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import useMyUserAccountByAccountExternalReferenceCode from '~/features/project/pages/Project/TeamMembers/components/TeamMembersTable/hooks/useMyUserAccountByAccountExternalReferenceCode';
import useCurrentKoroneikiAccount from '~/hooks/useCurrentKoroneikiAccount';

export default function useAtLeastOneProject() {
	const {data, loading} = useCurrentKoroneikiAccount();
	const koroneikiAccount = data?.koroneikiAccountByExternalReferenceCode;

	const {data: myUserAccountData} =
		useMyUserAccountByAccountExternalReferenceCode(
			koroneikiAccount?.accountKey,
			loading
		);
	const loggedUserAccount = myUserAccountData?.myUserAccount;

	const hasAtLeastOneProject = loggedUserAccount?.accountBriefs?.length > 0;

	return {
		atLeastOneProject: hasAtLeastOneProject,
		loading,
	};
}
