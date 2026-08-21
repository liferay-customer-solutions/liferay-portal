/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayInput} from '@clayui/form';
import ClayIcon from '@clayui/icon';

import {DetailedCard} from '../../../../../components/DetailedCard/DetailedCard';
import i18n from '../../../../../i18n';
import {Liferay} from '../../../../../liferay/liferay';
import {copyToClipboard} from '../../../../../utils/browser';

import './LDPTokenCard.scss';

type LDPTokenCardProps = {
	dataSourceAccessToken?: string;
	groupId?: number | string;
};

const LDPTokenCard: React.FC<LDPTokenCardProps> = ({
	dataSourceAccessToken,
	groupId,
}) => {
	// Provisioning stores the Analytics Cloud response verbatim in the
	// order-metadata custom field, so analyticsProject.dataSourceAccessToken
	// already carries the token and no request to Analytics Cloud is needed.
	// The groupId only tells whether the workspace exists yet.

	if (!groupId) {
		return null;
	}

	return (
		<DetailedCard
			cardIconAltText="Connect Icon"
			cardTitle={i18n.translate('connect-your-liferay-data-platform')}
			className="ldp-token-card"
			clayIcon="diagram"
		>
			<span className="ldp-token-card-label">
				{i18n.translate('copy-this-token-to-your-liferay-dxp-instance')}

				<span className="ldp-token-card-required">*</span>
			</span>

			{dataSourceAccessToken ? (
				<div className="ldp-token-card-field">
					<ClayInput
						className="ldp-token-card-input"
						readOnly
						value={dataSourceAccessToken}
					/>

					<button
						aria-label={i18n.translate('copy')}
						className="ldp-token-card-copy"
						onClick={() => {
							copyToClipboard(dataSourceAccessToken);

							Liferay.Util.openToast({
								message: i18n.sub(
									'copied-x-to-the-clipboard',
									'token'
								),
							});
						}}
						type="button"
					>
						<ClayIcon symbol="copy" />
					</button>
				</div>
			) : (
				<p className="ldp-token-card-message m-0">
					{i18n.translate(
						'the-data-source-token-is-not-available-yet-please-try-again-in-a-few-minutes'
					)}
				</p>
			)}
		</DetailedCard>
	);
};

export default LDPTokenCard;
