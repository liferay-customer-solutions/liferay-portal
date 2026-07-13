/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {ClayRadio} from '@clayui/form';
import classNames from 'classnames';
import {useEffect} from 'react';
import {useNavigate} from 'react-router-dom';
import AccountAvatar from '~/components/AccountAvatar/AccountAvatar';
import Loading from '~/components/Loading/Loading';
import i18n from '~/i18n';
import {useProductPurchaseLayoutContext} from '~/pages/ProductPurchase/components/ProductPurchaseLayout/ProductPurchaseLayout';
import ProductPurchaseShell from '~/pages/ProductPurchase/components/ProductPurchaseShell/ProductPurchaseShell';
import {Liferay} from '~/services/liferay/liferay';
import {
	ProductSpecificationKey,
	getLicenseTagText,
	getProductImageFallback,
	getProductPriceModel,
	getProductSpecificationValue,
} from '~/utils/productUtils';
import {normalizeURLProtocol} from '~/utils/stringUtils';

const HELP_CENTER_URL = 'https://help.liferay.com';

const AccountSelection = () => {
	const {
		accounts,
		actions: {nextStep},
		isLoadingAccounts,
		isSingleAccount,
		product,
		selectedAccount,
		setSelectedAccount,
	} = useProductPurchaseLayoutContext();

	const navigate = useNavigate();

	const {isPaidApp} = getProductPriceModel(product);

	useEffect(() => {
		if (!product) {
			return;
		}

		if (isSingleAccount) {
			if (selectedAccount?.id !== accounts[0]?.id) {
				setSelectedAccount(accounts[0]);
			}

			let solutionType = getProductSpecificationValue(
				ProductSpecificationKey.SOLUTION_TYPE,
				product
			);

			if (solutionType === 'ai-hub-open-beta') {
				navigate('/project', {replace: true});
			}
			else {
				navigate(isPaidApp ? '/license' : '/summary', {replace: true});
			}
		}
	}, [accounts, isPaidApp, isSingleAccount, navigate, setSelectedAccount, product, selectedAccount]);

	if (isLoadingAccounts || isSingleAccount) {
		return (
			<div className="d-flex justify-content-center my-5">
				<Loading />
			</div>
		);
	}

	const version = getProductSpecificationValue(
		ProductSpecificationKey.APP_VERSION,
		product
	);

	return (
		<ProductPurchaseShell
			footerProps={{
				backButtonProps: {className: 'd-none'},
				continueButtonProps: {
					disabled: !selectedAccount?.id,
					onClick: () => nextStep(),
				},
			}}
			title={i18n.translate('account-selection')}
		>
			<p className="text-muted">
				{i18n.sub(
					'accounts-available-for-x-you',
					Liferay.ThemeDisplay.getUserEmailAddress()
				)}
			</p>

			{accounts.length ? (
				accounts.map((account) => {
					const selected = selectedAccount?.id === account.id;

					return (
						<div
							className={classNames(
								'border mb-3 p-4 product-purchase-account-card rounded',
								{selected}
							)}
							key={account.id}
							onClick={() => setSelectedAccount(account)}
							role="button"
							tabIndex={0}
						>
							<div className="align-items-center d-flex">
								<ClayRadio
									checked={selected}
									className="mr-2"
									onChange={() => setSelectedAccount(account)}
									value={String(account.id)}
								/>

								<AccountAvatar
									logoURL={account.logoURL}
									type={account.type}
								/>

								<div className="ml-3">
									<strong className="d-block">
										{account.name}
									</strong>

									<small className="text-capitalize text-muted">
										{account.type}
									</small>
								</div>
							</div>

							{selected && (
								<div className="mt-3 product-purchase-account-card-summary pt-3">
									<small className="d-block font-weight-semi-bold mb-2 text-muted">
										{i18n.translate('licenses-summary')}
									</small>

									<div className="align-items-center d-flex justify-content-between">
										<div className="align-items-center d-flex">
											<img
												alt={product.name}
												className="object-fit-cover rounded"
												draggable={false}
												height="32px"
												src={
													normalizeURLProtocol(
														product.urlImage
													) ||
													getProductImageFallback(
														'productIcon'
													)
												}
												width="32px"
											/>

											<div className="ml-3">
												<strong className="d-block">
													{product.name}
												</strong>

												{version && (
													<small className="text-muted">
														{version}
													</small>
												)}
											</div>
										</div>

										<div className="text-right">
											<strong className="d-block">
												{i18n.translate('free')}
											</strong>

											<span className="badge badge-primary">
												{getLicenseTagText(product)}
											</span>
										</div>
									</div>
								</div>
							)}
						</div>
					);
				})
			) : (
				<p className="font-weight-bold my-5">
					{i18n.translate('no-accounts-available')}
				</p>
			)}

			<span className="mr-1 text-muted">
				{i18n.translate('not-seeing-a-specific-account')}
			</span>

			<a
				className="font-weight-bold"
				href={HELP_CENTER_URL}
				rel="noopener noreferrer"
				target="_blank"
			>
				{i18n.translate('contact-support')}
			</a>
		</ProductPurchaseShell>
	);
};

export default AccountSelection;
