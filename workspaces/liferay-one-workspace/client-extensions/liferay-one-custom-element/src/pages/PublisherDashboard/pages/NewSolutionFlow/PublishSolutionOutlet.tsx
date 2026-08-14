/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {useModal} from '@clayui/modal';
import {Link} from 'react-router-dom';
import Modal from '~/components/Modal/Modal';
import PublishModeContextProvider from '~/context/PublishModeContextProvider';
import {useSolutionContext} from '~/context/SolutionContextProvider';
import usePublishHeader from '~/hooks/usePublishHeader';
import usePublishSolutionSubmission from '~/hooks/usePublishSolutionSubmission';
import i18n from '~/i18n';
import usePublishNavigation from '~/pages/PublisherDashboard/hooks/usePublishNavigation';
import {ProductWorkflowStatusCode} from '~/utils/productUtils';

import BasePublishAppOutlet from '../../BasePublishAppOutlet';
import {PublishMode} from '../NewAppFlow/constants';
import {SOLUTIONS_EXIT_LINK, SOLUTION_FLOW_ITEMS} from './constants';

import type {SolutionInitialState} from '~/context/SolutionContextProvider';

const getFlowItems = (context: SolutionInitialState, mode: PublishMode) =>
	SOLUTION_FLOW_ITEMS.filter(
		(item) => item.modes.includes(mode) && item.visible(context)
	);

const isRequiredDraftFormFilled = (context: SolutionInitialState) =>
	SOLUTION_FLOW_ITEMS.filter((item) => item.saveAsDraftRequired).every(
		(item) => item.parseSchema && item.parseSchema(context).success
	);

const PublishSolutionOutlet = () => {
	usePublishHeader();

	const [context, dispatch] = useSolutionContext();
	const {observer, onOpenChange, open} = useModal();
	const onExitModal = useModal();

	const isEditingSolution =
		context?._product &&
		context._product.productStatus === ProductWorkflowStatusCode.APPROVED;

	const isSubmittedSolution =
		!!context?._product?.productId &&
		context._product.productStatus !== ProductWorkflowStatusCode.DRAFT;

	const publishMode = isSubmittedSolution
		? PublishMode.EDIT
		: PublishMode.CREATE;

	const {onSave, onSaveAsDraft} = usePublishSolutionSubmission(
		context,
		dispatch
	);

	const {onExit} = usePublishNavigation({
		exitLink: SOLUTIONS_EXIT_LINK,
		flowItems: getFlowItems(context, publishMode),
	});

	const canSaveAsDraft =
		!context?._product && isRequiredDraftFormFilled(context);

	if (context.loading) {
		return null;
	}

	return (
		<PublishModeContextProvider mode={publishMode}>
			<BasePublishAppOutlet
				canSaveAsDraft={canSaveAsDraft}
				context={context}
				flowItems={getFlowItems(context, publishMode)}
				isEditingApp={!!isEditingSolution}
				onClickExit={
					canSaveAsDraft
						? () => onOpenChange(true)
						: () => onExitModal.onOpenChange(true)
				}
				onSave={onSave}
				onSaveAsDraft={onSaveAsDraft}
			>
				<Modal
					last={
						<>
							<ClayButton
								disabled={!canSaveAsDraft}
								displayType="secondary"
								onClick={() => onSaveAsDraft().then(onExit)}
							>
								{i18n.translate('save-as-a-draft-exit')}
							</ClayButton>

							<Link
								className="btn btn-primary ml-2"
								to={SOLUTIONS_EXIT_LINK}
							>
								{i18n.translate('exit')}
							</Link>
						</>
					}
					observer={observer}
					title="Exit from creating a solution"
					visible={open}
				>
					<p>
						{i18n.translate(
							'all-progress-and-information-related-to-the-creation-of-the-solution-will-be-lost-unless-you-save-the-solution-as-a-draft-do-you-still-want-to-exit'
						)}
					</p>
				</Modal>

				{onExitModal.open && (
					<Modal
						last={
							<ClayButton
								className="btn btn-primary ml-2"
								displayType="primary"
								onClick={onExit}
							>
								{i18n.translate('exit')}
							</ClayButton>
						}
						observer={onExitModal.observer}
						title="Exit from creating a solution"
						visible={onExitModal.open}
					>
						<p>
							{i18n.translate(
								'all-progress-and-information-related-to-the-creation-of-the-solution-will-be-lost-do-you-still-want-to-exit'
							)}
						</p>
					</Modal>
				)}
			</BasePublishAppOutlet>
		</PublishModeContextProvider>
	);
};

export default PublishSolutionOutlet;
