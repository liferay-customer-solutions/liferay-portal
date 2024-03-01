/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import LiferayFile from '../../../common/interfaces/liferayFile';
import MDFClaimDocument from '../../../common/interfaces/mdfClaimDocument';
import createMDFClaimDocument from '../../../common/services/liferay/object/mdf-claim-documents/createMDFClaimDocument';
import updateMDFClaimDocument from '../../../common/services/liferay/object/mdf-claim-documents/updateMDFClaimDocument';

const submitMDFClaimDocuments = async (
	reimbursementInvoices: LiferayFile[] & MDFClaimDocument[],
	companyId: number,
	dtoMDFClaimId: number
) => {
	const dtoMDFClaimDocumentsCreate: LiferayFile[] & number[] = [];
	const dtoMDFClaimDocumentsUpdate: LiferayFile[] & number[] = [];

	if (reimbursementInvoices?.length) {
		reimbursementInvoices.map(async (reimbursementInvoice) => {
			if (reimbursementInvoice.documentId) {
				const dtoMDFClaimDocument = {
					id: reimbursementInvoice.claimDocumentId,
					r_accToMDFClmDocs_accountEntryId: companyId,
					r_mdfClmToMDFClmDocs_c_mdfClaimId: dtoMDFClaimId,
					reimbursementInvoices: reimbursementInvoice.documentId,
				};

				dtoMDFClaimDocument.id
					? dtoMDFClaimDocumentsUpdate.push(dtoMDFClaimDocument)
					: dtoMDFClaimDocumentsCreate.push(dtoMDFClaimDocument);
			}
		});
	}

	if (dtoMDFClaimDocumentsCreate.length) {
		await createMDFClaimDocument(dtoMDFClaimDocumentsCreate);
	}

	if (dtoMDFClaimDocumentsUpdate.length) {
		await updateMDFClaimDocument(dtoMDFClaimDocumentsUpdate);
	}
};

export default submitMDFClaimDocuments;
