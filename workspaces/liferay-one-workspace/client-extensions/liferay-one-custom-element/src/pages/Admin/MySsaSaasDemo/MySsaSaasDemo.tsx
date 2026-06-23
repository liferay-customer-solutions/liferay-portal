/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useModal} from '@clayui/modal';
import EmptyState from '~/components/EmptyState/EmptyState';
import Page from '~/components/Page/Page';
import {useOneContext} from '~/context/OneContextProvider';
import useModalContext from '~/hooks/useModalContext';
import i18n from '~/i18n';
import TrialListView from '~/pages/Admin/SSADashboard/components/TrialListView/TrialListView';
import useSSAActions from '~/pages/Admin/SSADashboard/hooks/useSSAActions';
import {useSSADashboardOutlet} from '~/pages/Admin/SSADashboard/hooks/useSSADashboardOutlet';

export default function MySsaSaasDemo() {
	const {userAccountModel} = useOneContext();
	const {onOpenModal} = useModalContext();
	const {myTrialsInProgress} = useSSADashboardOutlet();
	const actions = useSSAActions();
	const createTrialFormModal = useModal();

	const isSSAAdmin = userAccountModel.isSSAAdmin;

	const canCreateTrial = isSSAAdmin ? true : myTrialsInProgress < 3;

	const hasSSAPermission = isSSAAdmin || userAccountModel.isSSAUser;

	return (
		<Page
			description={i18n.translate('manage-your-current-trials')}
			pageRendererProps={{className: 'border py-2'}}
			rightButton={
				<ClayButton
					disabled={!hasSSAPermission}
					onClick={() => {
						if (canCreateTrial) {
							return createTrialFormModal.onOpenChange(true);
						}

						onOpenModal({
							body: (
								<span>
									{i18n.translate(
										'you-have-reached-the-maximum-number-of-active-trials-allowed-to-start-a-new-trial-please-end-one-of-your-existing-trials-first'
									)}
								</span>
							),
							header: i18n.translate('ssa-trials-limit-reached'),
						});
					}}
				>
					{i18n.translate('add-new-trial')}
				</ClayButton>
			}
			title={i18n.translate('my-saas-demos')}
		>
			{hasSSAPermission ? (
				<TrialListView
					actions={actions}
					authorOnlyTrials
					createTrialFormModal={createTrialFormModal}
					isSortable
					managementToolbarProps={{
						searchVisible: true,
						visible: isSSAAdmin,
					}}
				/>
			) : (
				<EmptyState
					description={
						<p>
							Reach out to the <strong>#help-ssa</strong> channel
							on slack for permission to continue
						</p>
					}
					title={i18n.translate('access-required')}
					type="NO_ACCESS"
				/>
			)}
		</Page>
	);
}
