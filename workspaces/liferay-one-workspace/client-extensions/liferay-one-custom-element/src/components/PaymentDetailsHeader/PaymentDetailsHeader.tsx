/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import Button from '@clayui/button';
import BackLink from '~/components/BackLink/BackLink';
import PaymentStatusBadge from '~/components/PaymentStatusBadge/PaymentStatusBadge';
import i18n from '~/i18n';

type PaymentDetailsHeaderProps = {
	backLink: string;
	onClick: () => void;
	paymentStatusCode: number;
	showButton: boolean;
	title: string;
};

const PaymentDetailsHeader = ({
	backLink,
	onClick,
	paymentStatusCode,
	showButton,
	title,
}: PaymentDetailsHeaderProps) => {
	return (
		<div className="align-items-center d-flex justify-content-between">
			<div>
				<BackLink path={backLink}>
					{i18n.translate('back-to-last-transaction')}
				</BackLink>

				<h2 className="mt-2">{title}</h2>

				<PaymentStatusBadge paymentStatus={paymentStatusCode} />
			</div>

			{showButton && (
				<Button displayType="secondary" onClick={onClick}>
					{i18n.translate('mark-as-paid')}
				</Button>
			)}
		</div>
	);
};

export default PaymentDetailsHeader;
