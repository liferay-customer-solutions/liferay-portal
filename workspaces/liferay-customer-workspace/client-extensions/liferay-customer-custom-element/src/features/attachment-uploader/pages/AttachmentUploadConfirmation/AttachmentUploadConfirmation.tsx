/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import i18n from '~/utils/I18n';

import './AttachmentUploadConfirmation.css';

import {useLocation} from 'react-router-dom';
import routerPath from '~/utils/routerPath';

const AttachmentUploadConfirmation = () => {
	const {state} = useLocation();
	const pageRoutes = routerPath();

	return (
		<div className="uploader-confirmation-container">
			<div className="uploader-confirmation-box-containter">
				<div className="d-flex justify-content-center pb-4">
					<div className="uploader-icon">
						<ClayIcon symbol="check-square" />
					</div>
				</div>

				<div className="d-flex justify-content-center pb-4 text-neutral-10">
					<h3>{i18n.translate('upload-confirmation')}</h3>
				</div>

				<div className="d-flex justify-content-center pb-5">
					<p
						className="text-center"
						dangerouslySetInnerHTML={{
							__html: i18n.sub(`x-was-uploaded-successfully`, [
								`<strong>${state?.attachmentName}</strong> <br>`,
							]),
						}}
					/>
				</div>

				<div>
					<div className="d-flex justify-content-center">
						<a
							className="btn btn-secondary mr-2 uploader-attachments-button"
							href={`${pageRoutes.project(state?.uploadAccountKey)}/attachments`}
						>
							{i18n.translate('go-to-attachments')}
						</a>

						<a
							className="btn btn-primary uploader-ticket-button"
							href={`https://help.liferay.com/hc/requests/${state?.ticketId}`}
						>
							{i18n.translate('return-to-ticket')}
						</a>
					</div>
				</div>
			</div>
		</div>
	);
};

export default AttachmentUploadConfirmation;
