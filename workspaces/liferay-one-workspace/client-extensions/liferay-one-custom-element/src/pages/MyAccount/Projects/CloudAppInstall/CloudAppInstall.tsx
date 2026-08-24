/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {zodResolver} from '@hookform/resolvers/zod';
import {useMemo, useState} from 'react';
import {useForm} from 'react-hook-form';
import {useNavigate, useParams} from 'react-router-dom';
import {z} from 'zod';
import Loading from '~/components/Loading/Loading';
import ProductPurchase from '~/components/ProductPurchase/ProductPurchase';
import {ProductSpecificationKey} from '~/enums/Product';
import useGetProductByOrderId from '~/hooks/useGetProductByOrderId';
import useGetResourceInfo from '~/hooks/useGetResourceInfo';
import i18n from '~/i18n';
import Console from '~/services/spring-boot/Console';
import {scrollToTop} from '~/utils/browserUtils';
import {convertSize} from '~/utils/fileUtils';
import {getProductSpecificationValue} from '~/utils/productUtils';

import EnvironmentSelection from './EnvironmentSelection/EnvironmentSelection';
import Installation from './Installation/Installation';
import ProjectSelection from './ProjectSelection/ProjectSelection';
import SelectedProjectBanner from './SelectedProjectBanner/SelectedProjectBanner';

import type {ConsoleUserProject} from '~/services/spring-boot/Console';
import type {PlacedOrder} from '~/types/orders';
import type {DeliveryProduct} from '~/types/product';

const resourcesSchema = z.object({
	free: z.number(),
	limit: z.number(),
	used: z.number(),
});

const installAppSchema = z.object({
	environment: z.object({
		isExtensionEnvironment: z.boolean(),
		projectId: z.string(),
	}),
	project: z.object({
		availabilityToProduct: z.boolean(),
		environments: z.array(
			z.object({
				isExtensionEnvironment: z.boolean(),
				projectId: z.string(),
			})
		),
		rootProjectId: z.string(),
		rootProjectPlanUsage: z.object({
			cpu: resourcesSchema,
			instance: resourcesSchema,
			memory: resourcesSchema,
		}),
	}),
});

export type InstallAppForm = z.infer<typeof installAppSchema>;

export type ConsoleUserProjectWithAvailability = ConsoleUserProject & {
	availabilityToProduct: boolean;
};

type InstallStep = 'environment' | 'installation' | 'project';

const verifyAvailabilityToInstall = (
	productRequirements: {cpu: string; ram: string},
	userProject: ConsoleUserProject
) => {
	const availableCPU = userProject?.rootProjectPlanUsage?.cpu?.free;
	const availableRAM = userProject?.rootProjectPlanUsage?.memory?.free;

	return (
		availableCPU >= Number(productRequirements.cpu) &&
		availableRAM >= convertSize('GB', 'MB', productRequirements.ram)
	);
};

const CloudAppInstall = () => {
	const {applicationERC, orderId} = useParams();
	const navigate = useNavigate();
	const [step, setStep] = useState<InstallStep>('project');

	const {data, error, isLoading} = useGetProductByOrderId(orderId as string);

	const form = useForm<InstallAppForm>({
		resolver: zodResolver(installAppSchema),
	});

	const product = data?.product as DeliveryProduct;
	const placedOrder = data?.placedOrder as PlacedOrder;

	const resourceResponse = useGetResourceInfo({
		product,
		shouldFetch: true,
	});

	const productRequirements = useMemo(
		() => ({
			cpu: getProductSpecificationValue(
				ProductSpecificationKey.APP_BUILD_NUMBER_OF_CPUS,
				product,
				'0'
			),
			ram: getProductSpecificationValue(
				ProductSpecificationKey.APP_BUILD_RAM_IN_GBS,
				product,
				'0'
			),
		}),
		[product]
	);

	const projects = useMemo(
		() =>
			resourceResponse?.resourceRequest?.userProjects.map(
				(userProject) => ({
					...userProject,
					availabilityToProduct: verifyAvailabilityToInstall(
						productRequirements,
						userProject
					),
				})
			) ?? [],
		[productRequirements, resourceResponse?.resourceRequest?.userProjects]
	);

	const project = form.watch('project');

	const onClickCancel = () => navigate(`../${applicationERC}?tab=activation`);

	const onSubmit = async ({environment}: InstallAppForm) => {
		scrollToTop();

		setStep('installation');

		await Console.provisioning(
			{
				orderItemId: placedOrder.placedOrderItems[0].id,
				projectId: environment.projectId,
			},
			placedOrder.id
		);
	};

	if (isLoading) {
		return <Loading />;
	}

	if (error || !product) {
		return (
			<p className="text-neutral-7">
				{i18n.translate('an-unexpected-error-occurred')}
			</p>
		);
	}

	return (
		<ProductPurchase>
			<ProductPurchase.Header
				product={product}
				rightNode={
					<div className="d-flex flex-column">
						<div>Standard License</div>

						<small className="d-flex justify-content-end">
							{`${productRequirements.cpu}CPUs, ${productRequirements.ram}GB RAM`}
						</small>
					</div>
				}
			>
				{project && <SelectedProjectBanner project={project} />}
			</ProductPurchase.Header>

			<ProductPurchase.Steps
				className="mt-5"
				steps={[
					{
						active: step === 'project',
						key: 'project',
						title: i18n.translate('project'),
					},
					{
						active: step === 'environment',
						key: 'environment',
						title: i18n.translate('environment'),
					},
					{
						active: step === 'installation',
						key: 'installation',
						title: i18n.translate('installation'),
					},
				]}
			/>

			<ProductPurchase.Body className="mt-5">
				{step === 'project' && (
					<ProjectSelection
						form={form}
						onClickCancel={onClickCancel}
						onClickContinue={() => setStep('environment')}
						projects={projects}
					/>
				)}

				{step === 'environment' && (
					<EnvironmentSelection
						form={form}
						onClickBack={() => setStep('project')}
						onClickCancel={onClickCancel}
						onSubmit={form.handleSubmit(onSubmit)}
						placedOrder={placedOrder}
					/>
				)}

				{step === 'installation' && (
					<Installation
						form={form}
						onClickBackToActivation={onClickCancel}
					/>
				)}
			</ProductPurchase.Body>
		</ProductPurchase>
	);
};

export default CloudAppInstall;
