/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {Dispatch} from 'react';
import {useNavigate} from 'react-router-dom';
import {useMarketplaceContext} from '~/context/MarketplaceContextProvider';
import {
	AppActions,
	SolutionInitialState,
	SolutionTypes,
} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';
import {SOLUTIONS_EXIT_LINK} from '~/pages/PublisherDashboard/pages/NewSolutionFlow/constants';
import SolutionPublish, {
	SolutionConfig,
} from '~/services/actions/SolutionPublish';
import {Liferay} from '~/services/liferay/liferay';

const usePublishSolutionSubmission = (
	context: SolutionInitialState,
	dispatch: Dispatch<AppActions>
) => {
	const navigate = useNavigate();
	const {myUserAccount} = useMarketplaceContext();

	const _onSave = async (config: Partial<SolutionConfig>) => {
		try {
			dispatch({payload: true, type: SolutionTypes.SET_LOADING});

			const solutionPublish = new SolutionPublish(context);

			const product = await solutionPublish.sync({
				...config,
				editorName: myUserAccount?.name ?? '',
			} as SolutionConfig);

			dispatch({payload: product, type: SolutionTypes.SET_PRODUCT});

			dispatch({payload: false, type: SolutionTypes.SET_LOADING});

			return product;
		}
		catch (error) {
			dispatch({payload: false, type: SolutionTypes.SET_LOADING});

			Liferay.Util.openToast({
				message: i18n.translate('an-unexpected-error-occurred'),
				type: 'danger',
			});

			throw error;
		}
	};

	const onSaveAsDraft = async () => {
		await _onSave({isDraft: true});

		Liferay.Util.openToast({
			message: i18n.sub('x-saved-as-a-draft-successfully', [
				context.profile.name,
			]),
			type: 'info',
		});

		navigate(SOLUTIONS_EXIT_LINK);
	};

	const onSave = async () => {
		await _onSave({isDraft: false});

		Liferay.Util.openToast({
			message: i18n.sub('solution-x-submitted', [context.profile.name]),
			title: '',
			type: 'info',
		});
	};

	return {onSave, onSaveAsDraft};
};

export default usePublishSolutionSubmission;
