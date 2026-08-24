/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import RadioCardList from '~/components/RadioCardList/RadioCardList';
import i18n from '~/i18n';
import {convertSize} from '~/utils/fileUtils';

import ContactSupport from '../ContactSupport/ContactSupport';
import WizardFooter from '../WizardFooter/WizardFooter';

import type {UseFormReturn} from 'react-hook-form';

import type {
	ConsoleUserProjectWithAvailability,
	InstallAppForm,
} from '../CloudAppInstall';

type ProjectSelectionProps = {
	form: UseFormReturn<InstallAppForm>;
	onClickCancel: () => void;
	onClickContinue: () => void;
	projects: ConsoleUserProjectWithAvailability[];
};

const hasExtensionEnvironment = (project: ConsoleUserProjectWithAvailability) =>
	project.environments.some(
		({isExtensionEnvironment}) => isExtensionEnvironment
	);

const isDisabled = (project: ConsoleUserProjectWithAvailability) =>
	!hasExtensionEnvironment(project) || !project.availabilityToProduct;

const ProjectSelection = ({
	form,
	onClickCancel,
	onClickContinue,
	projects,
}: ProjectSelectionProps) => {
	const selectedProject = form.watch('project');

	const sortedProjects = [...projects].sort((projectA, projectB) => {
		const aIsDisabled = isDisabled(projectA);
		const bIsDisabled = isDisabled(projectB);

		if (aIsDisabled && !bIsDisabled) {
			return 1;
		}

		if (!aIsDisabled && bIsDisabled) {
			return -1;
		}

		return projectA.rootProjectId.localeCompare(projectB.rootProjectId);
	});

	return (
		<ProductPurchase.Shell
			subtitle={
				<span
					dangerouslySetInnerHTML={{
						__html: i18n.sub('x-available-for-you', ['projects']),
					}}
				/>
			}
			title={i18n.translate('project-selection')}
		>
			<RadioCardList<ConsoleUserProjectWithAvailability>
				contentList={sortedProjects.map((project, index) => ({
					disabled: isDisabled(project),
					fullTitle: true,
					id: index,
					selected:
						selectedProject?.rootProjectId ===
						project.rootProjectId,
					title: (
						<div className="d-flex justify-content-between w-100">
							<div className="d-flex flex-column w-100">
								<div className="h5 m-0">
									{project.rootProjectId.toUpperCase()}
								</div>

								<p className="m-0 secondary-text">
									{`${project.environments.length} Environments, ${project.rootProjectPlanUsage.cpu.free}CPUs, ${convertSize(
										'MB',
										'GB',
										project.rootProjectPlanUsage.memory.free
									)}GB RAM`}
								</p>

								{!hasExtensionEnvironment(project) && (
									<small className="text-danger">
										{i18n.translate(
											'this-project-has-no-extension-environments'
										)}
									</small>
								)}

								{!project.availabilityToProduct && (
									<small className="text-danger">
										{i18n.translate(
											'the-selected-project-does-not-meet-the-necessary-resource-requirements-for-this-app-please-contact-sales-to-request-additional-resources'
										)}
									</small>
								)}
							</div>
						</div>
					),
					value: project,
				}))}
				leftRadio
				onSelect={(radioOption) => {
					if (isDisabled(radioOption.value)) {
						return;
					}

					form.resetField('environment');

					form.setValue('project', radioOption.value);
				}}
			/>

			<ContactSupport />

			<WizardFooter
				cancelButtonProps={{onClick: onClickCancel}}
				continueButtonProps={{
					disabled: !selectedProject,
					onClick: onClickContinue,
				}}
			/>
		</ProductPurchase.Shell>
	);
};

export default ProjectSelection;
