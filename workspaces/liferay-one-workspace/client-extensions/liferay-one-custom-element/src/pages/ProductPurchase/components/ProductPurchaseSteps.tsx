/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayIcon from '@clayui/icon';
import classNames from 'classnames';

export type ProductPurchaseStep = {
	active: boolean;
	title: string;
};

type ProductPurchaseStepsProps = {
	className?: string;
	steps: ProductPurchaseStep[];
};

const ProductPurchaseSteps = ({
	className,
	steps,
}: ProductPurchaseStepsProps) => {
	const activeStepIndex = steps.findIndex(({active}) => active);

	const getStepSymbol = (step: ProductPurchaseStep, index: number) => {
		if (step.active) {
			return 'radio-button';
		}

		if (index < activeStepIndex) {
			return 'check';
		}

		return 'simple-circle';
	};

	return (
		<div
			className={classNames(
				'd-flex justify-content-center product-purchase-steps text-nowrap',
				className
			)}
		>
			{steps.map((step, index) => (
				<div
					className={classNames('p-2 step', {
						done: index < activeStepIndex,
						selected: step.active,
					})}
					key={index}
				>
					<ClayIcon
						className="mr-2"
						symbol={getStepSymbol(step, index)}
					/>

					{step.title}
				</div>
			))}
		</div>
	);
};

export default ProductPurchaseSteps;
