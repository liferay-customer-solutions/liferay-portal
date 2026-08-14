/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import {ClaySelect} from '@clayui/form';
import ClayIcon from '@clayui/icon';
import ClayModal, {useModal} from '@clayui/modal';
import {useState} from 'react';
import ButtonWithIcon from '~/components/ButtonWithIcon/ButtonWithIcon';
import Form from '~/components/MarketplaceForm/MarketplaceForm';
import TextAndImages from '~/components/TextAndImages/TextAndImages';
import TextAndVideo from '~/components/TextAndVideo/TextAndVideo';
import TextBlock from '~/components/TextBlock/TextBlock';
import {
	BlockDirections,
	SolutionTypes,
	useSolutionContext,
} from '~/context/SolutionContextProvider';
import i18n from '~/i18n';

import {BLOCK_TYPES} from '../constants';

import '../NewSolutionFlow.css';

import type {ContentBlock} from '~/context/SolutionContextProvider';

const MINIMUM_BLOCKS = 2;

const BLOCK_OPTIONS = [
	{label: i18n.translate('choose-an-option'), value: ''},
	{
		label: i18n.translate('text-and-images-block'),
		value: BLOCK_TYPES.TEXT_IMAGES,
	},
	{
		label: i18n.translate('text-and-video-block'),
		value: BLOCK_TYPES.TEXT_VIDEO,
	},
	{label: i18n.translate('text-block'), value: BLOCK_TYPES.TEXT},
];

const Details = () => {
	const [{details: blocks}, dispatch] = useSolutionContext();
	const {observer, onOpenChange, open} = useModal();
	const [selectedBlock, setSelectedBlock] = useState('');

	const onDeleteImage = (id: string) =>
		dispatch({payload: id, type: SolutionTypes.SET_DELETE_IMAGE});

	const renderBlock = (block: ContentBlock, index: number) => {
		const onChange = (content: Partial<ContentBlock['content']>) =>
			dispatch({
				payload: {
					block: {
						...block,
						content: {...block.content, ...content},
					} as ContentBlock,
					index,
				},
				type: SolutionTypes.SET_UPDATE_BLOCK,
			});

		if (block.type === BLOCK_TYPES.TEXT) {
			return (
				<TextBlock
					block={block}
					onChange={onChange}
					onDeleteImage={onDeleteImage}
				/>
			);
		}

		if (block.type === BLOCK_TYPES.TEXT_IMAGES) {
			return (
				<TextAndImages
					block={block}
					onChange={onChange}
					onDeleteImage={onDeleteImage}
				/>
			);
		}

		return (
			<TextAndVideo
				block={block}
				onChange={onChange}
				onDeleteImage={onDeleteImage}
			/>
		);
	};

	return (
		<div className="new-solution-form-details">
			<Form.Label className="mt-3" required>
				{i18n.sub('add-a-minimum-of-x-blocks', `${MINIMUM_BLOCKS}`)}
			</Form.Label>

			{blocks.map((block, index) => {
				const onMoveOrDelete = (direction: BlockDirections) =>
					dispatch({
						payload: {direction, index},
						type: SolutionTypes.SET_BLOCK_MOVE,
					});

				const dropdownItems = [
					{
						disabled: index === 0,
						name: i18n.translate('move-to-top'),
						onClick: () =>
							onMoveOrDelete(BlockDirections.MOVE_TO_TOP),
					},
					{
						disabled: index === 0,
						name: i18n.translate('move-up'),
						onClick: () => onMoveOrDelete(BlockDirections.MOVE_UP),
					},
					{
						disabled: index === blocks.length - 1,
						name: i18n.translate('move-down'),
						onClick: () =>
							onMoveOrDelete(BlockDirections.MOVE_DOWN),
					},
					{
						disabled: index === blocks.length - 1,
						name: i18n.translate('move-to-bottom'),
						onClick: () =>
							onMoveOrDelete(BlockDirections.MOVE_TO_BOTTOM),
					},
					{
						name: i18n.translate('delete'),
						onClick: () => onMoveOrDelete(BlockDirections.DELETE),
					},
				];

				return (
					<Form.SectionWithControllers
						dropdownItems={dropdownItems}
						index={index}
						key={index}
						name={
							BLOCK_OPTIONS.find(
								({value}) => value === block.type
							)?.label as string
						}
						onArrowClick={onMoveOrDelete}
						position={blocks.length}
					>
						{renderBlock(block, index)}
					</Form.SectionWithControllers>
				);
			})}

			<ClayButton
				className="align-items-center d-flex flex-row justify-content-center mt-4 new-solution-form-details-add-block w-100"
				displayType="secondary"
				onClick={() => onOpenChange(true)}
			>
				<span className="d-flex flex-row inline-item inline-item-before">
					<ClayIcon symbol="plus" />
				</span>

				{i18n.translate('add-content-block')}
			</ClayButton>

			{open && (
				<ClayModal center observer={observer}>
					<ClayModal.Body className="mb-1">
						<h1 className="d-flex justify-content-between">
							{i18n.translate('select-content-block')}

							<ButtonWithIcon
								aria-label={i18n.translate('close')}
								className="inline-item"
								displayType="unstyled"
								onClick={() => onOpenChange(false)}
								size="sm"
								symbol="times"
								title={i18n.translate('close')}
							/>
						</h1>

						<p className="text-neutral-7">
							{i18n.translate(
								'choose-one-of-the-following-content-blocks'
							)}
						</p>

						<Form.Label
							className="mt-5"
							htmlFor="choose-block"
							required
						>
							{i18n.translate('choose-block')}
						</Form.Label>

						<ClaySelect
							aria-label={i18n.translate('choose-block')}
							id="choose-block"
							onChange={({target}) =>
								setSelectedBlock(target.value)
							}
							value={selectedBlock}
						>
							{BLOCK_OPTIONS.map((item, index) => (
								<ClaySelect.Option
									key={index}
									label={item.label}
									value={item.value}
								/>
							))}
						</ClaySelect>

						<div className="align-items-end d-flex justify-content-end mt-8">
							<ClayButton
								className="mr-2"
								displayType="secondary"
								onClick={() => onOpenChange(false)}
							>
								{i18n.translate('cancel')}
							</ClayButton>

							<ClayButton
								disabled={!selectedBlock}
								displayType="primary"
								onClick={() => {
									onOpenChange(false);

									dispatch({
										payload: {
											content: {},
											type: selectedBlock,
										} as unknown as ContentBlock,
										type: SolutionTypes.SET_NEW_BLOCK,
									});

									setSelectedBlock('');
								}}
							>
								{i18n.translate('save')}
							</ClayButton>
						</div>
					</ClayModal.Body>
				</ClayModal>
			)}
		</div>
	);
};

export default Details;
