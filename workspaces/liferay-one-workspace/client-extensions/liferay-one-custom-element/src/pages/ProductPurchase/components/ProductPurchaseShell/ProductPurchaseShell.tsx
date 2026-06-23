/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import classNames from 'classnames';
import {ComponentProps, ReactNode} from 'react';

import ProductPurchaseFooter from '../ProductPurchaseFooter/ProductPurchaseFooter';

type ProductPurchaseShellProps = {
	children: ReactNode;
	footerProps?: ComponentProps<typeof ProductPurchaseFooter>;
	title: string;
} & React.HTMLAttributes<HTMLDivElement>;

const ProductPurchaseShell = ({
	children,
	footerProps,
	title,
	...props
}: ProductPurchaseShellProps) => (
	<div
		{...props}
		className={classNames('product-purchase-shell', props.className)}
	>
		<h1 className="mb-4 product-purchase-shell-title text-center">
			{title}
		</h1>

		{children}

		{footerProps && <ProductPurchaseFooter {...footerProps} />}
	</div>
);

export default ProductPurchaseShell;
