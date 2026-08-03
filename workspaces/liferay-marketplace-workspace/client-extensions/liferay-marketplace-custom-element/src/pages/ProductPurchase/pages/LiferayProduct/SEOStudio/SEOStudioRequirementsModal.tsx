/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayModal, {useModal} from '@clayui/modal';
import {useState} from 'react';

import ButtonWithIcon from '../../../../../components/ButtonWithIcon';
import i18n from '../../../../../i18n';

import './SEOStudioRequirementsModal.scss';

type SEOStudioRequirementsModalProps = ReturnType<typeof useModal> & {
	onContinue: () => Promise<void>;
};

const SEOStudioRequirementsModal: React.FC<SEOStudioRequirementsModalProps> = ({
	observer,
	onContinue,
	onOpenChange,
}) => {
	const [loading, setLoading] = useState(false);

	const onContinueClick = async () => {
		setLoading(true);

		await onContinue().catch(console.error);

		setLoading(false);

		onOpenChange(false);
	};

	return (
		<ClayModal
			center
			className="seo-studio-requirements-modal"
			observer={observer}
		>
			<div className="pt-4 px-4">
				<div className="d-flex justify-content-between">
					<span className="seo-studio-requirements-modal-header">
						{i18n.translate('seo-studio')}
					</span>

					<ButtonWithIcon
						aria-labelledby="close icon"
						className="align-self-start"
						displayType="unstyled"
						onClick={() => onOpenChange(false)}
						symbol="times"
					/>
				</div>

				<h2 className="seo-studio-requirements-modal-title">
					{i18n.translate('requirements')}
				</h2>

				<p className="seo-studio-requirements-modal-description">
					{i18n.translate(
						'this-product-is-only-available-for-ai-hub-customers'
					)}
				</p>

				<div className="d-flex justify-content-end my-4">
					<ClayButton
						className="mr-3"
						displayType="secondary"
						onClick={() => onOpenChange(false)}
					>
						{i18n.translate('cancel')}
					</ClayButton>

					<ClayButton disabled={loading} onClick={onContinueClick}>
						{i18n.translate('continue')}
					</ClayButton>
				</div>
			</div>
		</ClayModal>
	);
};

export default SEOStudioRequirementsModal;
