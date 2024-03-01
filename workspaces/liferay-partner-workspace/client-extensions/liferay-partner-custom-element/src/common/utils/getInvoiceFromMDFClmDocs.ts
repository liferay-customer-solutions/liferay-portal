/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import MDFClaimDTO from '../interfaces/dto/mdfClaimDTO';
import LiferayFile from '../interfaces/liferayFile';
import MDFClaimDocument from '../interfaces/mdfClaimDocument';
import getNameFromMDFClaimDocument from './getNameFromMDFClaimDocument';

export default function getInvoiceFromMDFClmDocs(mdfClaimDto: MDFClaimDTO) {
	return mdfClaimDto.mdfClmToMDFClmDocs?.reduce(
		(accumulatorDocuments, currentDocument) => {
			const reimbursementInvoiceFile = {
				claimDocumentId: currentDocument.id,
				documentId: currentDocument.reimbursementInvoices?.id,
				link: currentDocument.reimbursementInvoices?.link,
				name:
					currentDocument.reimbursementInvoices?.name &&
					getNameFromMDFClaimDocument(
						currentDocument.reimbursementInvoices.name
					),
			};

			accumulatorDocuments?.push(reimbursementInvoiceFile);

			return accumulatorDocuments;
		},
		[] as LiferayFile[] & number[]
	) as MDFClaimDocument[];
}
