/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayBadge from '@clayui/badge';
import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import RadioCardList from '~/components/RadioCardList/RadioCardList';
import i18n from '~/i18n';
import {OrderCustomFields} from '~/utils/orderUtils';
import {safeJSONParse} from '~/utils/safeJSONParse';

import ContactSupport from '../ContactSupport/ContactSupport';
import WizardFooter from '../WizardFooter/WizardFooter';

import type {UseFormReturn} from 'react-hook-form';
import type {PlacedOrder} from '~/types/orders';

import type {Provisioning} from '../../components/AppProvisioning/types';
import type {InstallAppForm} from '../CloudAppInstall';

type EnvironmentSelectionProps = {
	form: UseFormReturn<InstallAppForm>;
	onClickBack: () => void;
	onClickCancel: () => void;
	onSubmit: () => void;
	placedOrder: PlacedOrder;
};

type ProjectEnvironment = InstallAppForm['environment'];

const EnvironmentSelection = ({
	form,
	onClickBack,
	onClickCancel,
	onSubmit,
	placedOrder,
}: EnvironmentSelectionProps) => {
	const selectedEnvironment = form.watch('environment');
	const selectedProject = form.watch('project');

	const cloudProvisioning = safeJSONParse<Provisioning[]>(
		(placedOrder.customFields ?? {})[
			OrderCustomFields.CLOUD_PROVISIONING
		] ?? null,
		[]
	);

	const isDeployed = (projectEnvironment: ProjectEnvironment) => {
		const [, environment = ''] = projectEnvironment.projectId.split('-');

		return cloudProvisioning.some((provisioning) =>
			provisioning.deployments.some((deployment) => {
				const [, deploymentEnvironment = ''] =
					deployment.projectId.split('-');

				return deploymentEnvironment === environment;
			})
		);
	};

	return (
		<ProductPurchase.Shell
			subtitle={
				<span
					dangerouslySetInnerHTML={{
						__html: i18n.sub('x-available-for-you', [
							'environments',
						]),
					}}
				/>
			}
			title={i18n.translate('environment-selection')}
		>
			<RadioCardList<ProjectEnvironment>
				contentList={(selectedProject?.environments ?? []).map(
					(projectEnvironment, index) => {
						const [projectName = '', environment = ''] =
							projectEnvironment.projectId.split('-');

						const disabled = isDeployed(projectEnvironment);

						return {
							disabled,
							fullTitle: true,
							id: index,
							selected:
								projectEnvironment.projectId ===
								selectedEnvironment?.projectId,
							title: (
								<>
									<div>
										<span className="h5 mr-3">
											{projectName.toUpperCase()}
										</span>

										<ClayBadge
											className="text-uppercase"
											label={environment}
										/>
									</div>

									{disabled && (
										<span className="text-danger">
											{i18n.translate(
												'this-app-is-already-installed-in-this-environment'
											)}
										</span>
									)}
								</>
							),
							value: projectEnvironment,
						};
					}
				)}
				leftRadio
				onSelect={(radioOption) => {
					if (isDeployed(radioOption.value)) {
						return;
					}

					form.setValue('environment', radioOption.value);
				}}
			/>

			<ContactSupport />

			<WizardFooter
				backButtonProps={{onClick: onClickBack}}
				cancelButtonProps={{onClick: onClickCancel}}
				continueButtonProps={{
					children: i18n.translate('install'),
					disabled: !selectedEnvironment,
					onClick: onSubmit,
				}}
			/>
		</ProductPurchase.Shell>
	);
};

export default EnvironmentSelection;
