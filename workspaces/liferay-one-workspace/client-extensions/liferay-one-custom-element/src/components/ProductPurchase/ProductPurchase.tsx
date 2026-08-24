/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayIcon from '@clayui/icon';
import ClayMultiStepNav from '@clayui/multi-step-nav';
import ClaySticker from '@clayui/sticker';
import classNames from 'classnames';
import {ComponentProps, ReactElement, ReactNode} from 'react';
import createdProjectIcon from '~/assets/images/created_project.svg';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext as useProductPurchaseOutletContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import {Liferay} from '~/services/liferay/liferay';
import {getSiteURL} from '~/utils/siteUtils';
import {normalizeURLProtocol} from '~/utils/stringUtils';

import './ProductPurchase.css';

import './ProductPurchaseFeedback.css';

import './StepWizard.css';

import type {Account} from '~/types/accounts';
import type {DeliveryProduct} from '~/types/product';

type ProductPurchaseBodyProps = React.HTMLAttributes<HTMLDivElement>;

const ProductPurchaseBody: React.FC<ProductPurchaseBodyProps> = ({
	children,
	className,
	...props
}) => (
	<div
		className={classNames(
			'border d-flex flex-column p-5 rounded',
			className
		)}
		{...props}
	>
		{children}
	</div>
);

type HighlightProps = {
	children: string;
};

const Highlight: React.FC<HighlightProps> = ({children}) => (
	<b className="highlight-text">{children}</b>
);

type ProductPurchaseFeedbackProps = {
	children: React.ReactNode;
	className?: string;
	description: string | React.ReactNode;
	title: string | React.ReactNode;
};

const ProductPurchaseFeedback: React.FC<ProductPurchaseFeedbackProps> & {
	Highlight: typeof Highlight;
} = ({children, className, description, title}) => (
	<div className="d-flex justify-content-center w-100">
		<div
			className={classNames(
				'align-items-center d-flex flex-column justify-content-center col-3',
				className
			)}
		>
			<img
				alt="project icon"
				className="gate-card-image mb-6"
				draggable={false}
				src={createdProjectIcon}
			/>

			<div className="my-5 text-center">
				<h1>{title}</h1>

				<span>{description}</span>
			</div>

			{children}
		</div>
	</div>
);

ProductPurchaseFeedback.Highlight = Highlight;

type ProductPurchaseFooterProps = {
	backButtonProps?: ComponentProps<typeof ClayButton>;
	cancelButtonProps?: ComponentProps<typeof ClayButton>;
	continueButtonProps?: ComponentProps<typeof ClayButton>;
	termsAndConditions?: ReactElement;
};

const ProductPurchaseFooter: React.FC<ProductPurchaseFooterProps> = ({
	backButtonProps,
	cancelButtonProps,
	continueButtonProps,
	termsAndConditions,
}) => {
	const {productPurchaseCart} = useProductPurchaseOutletContext();

	return (
		<div className="d-flex flex-column mt-3 w-100">
			<div className="align-items-center d-flex justify-content-between mt-3 w-100">
				<ClayButton
					className="font-weight-semi-bold"
					displayType="unstyled"
					onClick={() => {
						if (productPurchaseCart.cart.id) {
							productPurchaseCart.removeCart(
								productPurchaseCart.cart.id
							);
						}
						Liferay.Util.navigate(getSiteURL() || '/');
					}}
					{...cancelButtonProps}
				>
					{cancelButtonProps?.children || i18n.translate('cancel')}
				</ClayButton>

				<div>
					<ClayButton displayType="secondary" {...backButtonProps}>
						{backButtonProps?.children || i18n.translate('back')}
					</ClayButton>

					<ClayButton className="ml-4" {...continueButtonProps}>
						{continueButtonProps?.children ||
							i18n.translate('continue')}
					</ClayButton>
				</div>
			</div>

			{termsAndConditions && (
				<div className="d-flex justify-content-end pt-3 text-black-50 w-100">
					{termsAndConditions}
				</div>
			)}
		</div>
	);
};

type ProductPurchaseHeaderAccountProps = {
	account: Account;
};

type ProductPurchaseHeaderProps = {
	children?: ReactNode;
	product: DeliveryProduct;
	rightNode?: ReactNode;
};

const AccountEmailInfo: React.FC<{image?: string; name?: string}> = ({
	image,
	name,
}) => (
	<div className="align-items-center d-flex">
		<div className="account-banner-name-text align-items-end d-flex flex-column mx-2">
			<strong>{name}</strong>

			<div className="account-banner-email-text">
				{Liferay.ThemeDisplay.getUserEmailAddress()}
			</div>
		</div>

		<ClaySticker displayType="light" shape="circle" size="sm">
			{image ? (
				<ClaySticker.Image
					alt="placeholder"
					draggable={false}
					height={24}
					src={image}
					width={24}
				/>
			) : (
				<ClayIcon symbol="picture" />
			)}
		</ClaySticker>
	</div>
);

const ProductPurchaseHeaderAccount: React.FC<
	ProductPurchaseHeaderAccountProps
> = ({account}) => {
	if (!account) {
		return null;
	}

	return (
		<>
			<hr className="mx-n5" />

			<div className="d-flex flex-row justify-content-between">
				<strong className="account-banner-title-text align-self-center">
					{i18n.translate('account-selected')}
				</strong>

				<AccountEmailInfo image={account.logoURL} name={account.name} />
			</div>
		</>
	);
};

const ProductPurchaseHeader: React.FC<ProductPurchaseHeaderProps> = ({
	children,
	product,
	rightNode,
}) => {
	const HeadingComponent = product.name.length > 30 ? 'h3' : 'h1';

	return (
		<div className="product-banner px-5 py-5">
			<div className="d-flex flex-row justify-content-between">
				<div className="d-flex flex-row">
					<img
						alt="App Icon"
						className="object-fit-cover rounded"
						draggable={false}
						height="64px"
						src={normalizeURLProtocol(product.urlImage)}
						width="64px"
					/>

					<div className="align-items-center ml-4">
						<HeadingComponent className="font-weight-semi-bold product-banner-title">
							{product.name}
						</HeadingComponent>

						<span className="sub-text">{product.catalogName}</span>
					</div>
				</div>

				{rightNode}
			</div>

			{children}
		</div>
	);
};

type ProductPurchasePriceProps = {
	children: ReactNode;
	price: number | string;
} & React.HTMLAttributes<HTMLDivElement>;

const ProductPurchasePrice: React.FC<ProductPurchasePriceProps> = ({
	children,
	price,
	...priceProps
}) => (
	<div className="align-items-end d-flex flex-column price-text">
		<strong className="mr-1 price-text-label">
			{i18n.translate('price')}
		</strong>

		<div
			{...priceProps}
			className={classNames('price-text-value', priceProps.className)}
			style={priceProps.style}
		>
			{price}
		</div>

		{children}
	</div>
);

type ProductPurchaseShellProps = {
	as?: React.ElementType;
	children: ReactNode;
	footerProps?: ComponentProps<typeof ProductPurchaseFooter>;
	subtitle?: ReactNode | string;
	title: string;
} & React.HTMLAttributes<HTMLElement>;

const ProductPurchaseShell: React.FC<ProductPurchaseShellProps> = ({
	as: Component = 'div',
	children,
	footerProps,
	subtitle,
	title,
	...props
}) => (
	<Component
		{...props}
		className={classNames('product-purchase-shell', props.className)}
	>
		<div className="mb-3 product-purchase-shell-heading">
			<h1 className="m-0 text-center text-weight-bold">{title}</h1>
			{subtitle && <span>{subtitle}</span>}
		</div>

		{children}

		{footerProps && <ProductPurchaseFooter {...footerProps} />}
	</Component>
);

type Step = {
	active: boolean;
	key: string;
	subTitle?: string;
	title: string;
};

type ProductPurchaseStepsProps = {
	className?: string;
	onClickIndicator?: (step: Step) => void;
	steps: Step[];
};

const ProductPurchaseSteps: React.FC<ProductPurchaseStepsProps> = ({
	className,
	onClickIndicator = () => null,
	steps,
}) => (
	<ClayMultiStepNav
		className={classNames(
			'mx-6 product-purchase--multi-step-nav',
			className
		)}
	>
		{steps.map((step, index) => (
			<ClayMultiStepNav.Item
				active={step.active}
				expand={index + 1 !== steps.length}
				key={index}
				state={
					steps.findIndex(({active}) => active) > index
						? 'complete'
						: undefined
				}
			>
				<ClayMultiStepNav.Title>{step.title}</ClayMultiStepNav.Title>
				<ClayMultiStepNav.Divider />
				<ClayMultiStepNav.Indicator
					label={1 + index}
					onClick={() => onClickIndicator(step)}
					subTitle={step.subTitle}
				/>
			</ClayMultiStepNav.Item>
		))}
	</ClayMultiStepNav>
);

type CircleStep = {
	active: boolean;
	index?: boolean;
	key: string;
	subTitle?: string;
	title: string;
};

type CircleStepsProps = {
	className?: string;
	steps: CircleStep[];
};

const CircleSteps: React.FC<CircleStepsProps> = ({className, steps}) => {
	const activeStepIndex = steps.findIndex(({active}) => active);

	const stepIcon = (step: CircleStep, index: number) => {
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
				'd-flex justify-content-center step-wizard text-nowrap',
				className
			)}
		>
			{steps.map((step, index) => (
				<div
					className={classNames('step p-2', {
						done: index < activeStepIndex,
						selected: step.active,
					})}
					key={index}
				>
					<ClayIcon
						className={classNames('mr-2 step', {
							done: index < activeStepIndex,
							selected: step.active,
						})}
						symbol={stepIcon(step, index)}
					/>

					{step.title}
				</div>
			))}
		</div>
	);
};

type ProductPurchaseProps = {
	children: ReactNode;
} & React.HTMLAttributes<HTMLDivElement>;

type ProductPurchaseChildrens = {
	Body: typeof ProductPurchaseBody;
	CircleSteps: typeof CircleSteps;
	Feedback: typeof ProductPurchaseFeedback;
	Footer: typeof ProductPurchaseFooter;
	Header: typeof ProductPurchaseHeader;
	HeaderAccount: typeof ProductPurchaseHeaderAccount;
	Price: typeof ProductPurchasePrice;
	Shell: typeof ProductPurchaseShell;
	Steps: typeof ProductPurchaseSteps;
};

const ProductPurchase: React.FC<ProductPurchaseProps> &
	ProductPurchaseChildrens = ({children, className, ...props}) => (
	<div
		{...props}
		className={classNames('container', className)}
		style={{width: 600}}
	>
		{children}
	</div>
);

ProductPurchase.Body = ProductPurchaseBody;
ProductPurchase.CircleSteps = CircleSteps;
ProductPurchase.Feedback = ProductPurchaseFeedback;
ProductPurchase.Footer = ProductPurchaseFooter;
ProductPurchase.Header = ProductPurchaseHeader;
ProductPurchase.HeaderAccount = ProductPurchaseHeaderAccount;
ProductPurchase.Price = ProductPurchasePrice;
ProductPurchase.Shell = ProductPurchaseShell;
ProductPurchase.Steps = ProductPurchaseSteps;

export default ProductPurchase;
