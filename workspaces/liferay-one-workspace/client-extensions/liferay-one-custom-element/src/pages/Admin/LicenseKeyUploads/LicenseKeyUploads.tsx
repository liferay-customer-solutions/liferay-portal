/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import ClayButton from '@clayui/button';
import ClayDropDown, {Align} from '@clayui/drop-down';
import {useModal} from '@clayui/modal';
import ClayTabs from '@clayui/tabs';
import {ChangeEvent, FormEvent, useRef, useState} from 'react';
import {useSearchParams} from 'react-router-dom';
import ButtonWithIcon from '~/components/ButtonWithIcon/ButtonWithIcon';
import Loading from '~/components/Loading/Loading';
import Modal from '~/components/Modal/Modal';
import Page from '~/components/Page/Page';
import Table from '~/components/Table/Table';
import i18n from '~/i18n';
import FetcherError from '~/services/fetcher/FetcherError';
import {Liferay} from '~/services/liferay/liferay';
import CommonLicenseKeys, {
	CommonLicenseKey,
	ProductGroup,
} from '~/services/spring-boot/CommonLicenseKeys';
import {formatDate} from '~/utils/dateUtils';

import useCommonLicenseKeys, {PAGE_SIZE} from './hooks/useCommonLicenseKeys';

type Tab = 'commerce' | 'elasticsearch';

const PRODUCT_GROUP_BY_TAB: Record<Tab, ProductGroup> = {
	commerce: 'COMMERCE',
	elasticsearch: 'ENTERPRISE_SEARCH',
};

const TABS: Tab[] = ['commerce', 'elasticsearch'];

function getUploadErrorMessage(error: unknown): string {
	const info = (error as FetcherError)?.info;

	if (
		info?.type?.includes('DuplicateCommonLicenseKeyException') ||
		info?.title?.includes('DuplicateCommonLicenseKey')
	) {
		return i18n.translate('the-file-has-already-been-uploaded');
	}

	return (
		info?.title ??
		info?.detail ??
		i18n.translate('an-unexpected-error-occurred')
	);
}

type LicenseKeyUploadsPanelProps = {
	productGroup: ProductGroup;
};

function LicenseKeyUploadsPanel({productGroup}: LicenseKeyUploadsPanelProps) {
	const [page, setPage] = useState(1);
	const [selectedFiles, setSelectedFiles] = useState<File[]>([]);
	const [selectedKey, setSelectedKey] = useState<CommonLicenseKey>();
	const [uploadError, setUploadError] = useState<string>();
	const [uploading, setUploading] = useState(false);

	const fileInputRef = useRef<HTMLInputElement>(null);
	const deleteModal = useModal();

	const {data, isLoading, mutate} = useCommonLicenseKeys(productGroup, page);

	function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
		setSelectedFiles(Array.from(event.target.files ?? []));
		setUploadError(undefined);
	}

	async function handleUpload(event: FormEvent) {
		event.preventDefault();

		if (!selectedFiles.length) {
			return;
		}

		setUploading(true);
		setUploadError(undefined);

		try {
			await CommonLicenseKeys.uploadCommonLicenseKeys(
				productGroup,
				selectedFiles
			);

			setSelectedFiles([]);

			if (fileInputRef.current) {
				fileInputRef.current.value = '';
			}

			setPage(1);

			await mutate();

			Liferay.Util.openToast({
				message: i18n.translate('license-keys-uploaded-successfully'),
				type: 'success',
			});
		}
		catch (error) {
			setUploadError(getUploadErrorMessage(error));
		}
		finally {
			setUploading(false);
		}
	}

	async function handleConfirmDelete() {
		if (!selectedKey) {
			return;
		}

		try {
			await CommonLicenseKeys.deleteCommonLicenseKey(selectedKey.id);

			if (data?.items.length === 1 && page > 1) {
				setPage(page - 1);
			}

			await mutate();

			Liferay.Util.openToast({
				message: i18n.translate('license-key-deleted-successfully'),
				type: 'success',
			});
		}
		catch {
			Liferay.Util.openToast({
				message: i18n.translate('an-unexpected-error-occurred'),
				type: 'danger',
			});
		}
		finally {
			deleteModal.onOpenChange(false);

			setSelectedKey(undefined);
		}
	}

	const items = data?.items ?? [];
	const totalCount = data?.totalCount ?? 0;

	const RowActions = ({row}: {row: CommonLicenseKey}) => (
		<ClayDropDown
			alignmentPosition={Align.BottomCenter}
			closeOnClick
			items={[
				{
					label: i18n.translate('download'),
					onClick: () =>
						CommonLicenseKeys.downloadCommonLicenseKey(
							row.id,
							row.name
						),
				},
				{
					label: i18n.translate('delete'),
					onClick: () => {
						setSelectedKey(row);

						deleteModal.onOpenChange(true);
					},
				},
			]}
			trigger={
				<ButtonWithIcon
					aria-label={i18n.translate('actions')}
					className="btn-monospaced"
					displayType="unstyled"
					symbol="ellipsis-v"
				/>
			}
		>
			{(item, index) => (
				<ClayDropDown.Item
					onClick={() => item.onClick()}
					{...{['keyValue']: index}}
				>
					{item.label}
				</ClayDropDown.Item>
			)}
		</ClayDropDown>
	);

	return (
		<>
			<form className="mb-4" onSubmit={handleUpload}>
				<div className="align-items-center d-flex">
					<input
						accept=".xml"
						className="form-control mr-3"
						multiple
						onChange={handleFileChange}
						ref={fileInputRef}
						style={{maxWidth: '24rem'}}
						type="file"
					/>

					<ClayButton
						disabled={!selectedFiles.length || uploading}
						type="submit"
					>
						{uploading
							? i18n.translate('uploading')
							: i18n.translate('upload')}
					</ClayButton>
				</div>

				{uploadError && (
					<div className="mt-3 text-danger">{uploadError}</div>
				)}
			</form>

			{isLoading ? (
				<Loading />
			) : (
				<Table
					Actions={
						RowActions as React.FC<{row: Record<string, unknown>}>
					}
					columns={[
						{key: 'name', title: i18n.translate('name')},
						{
							key: 'productEnvironment',
							title: i18n.translate('product-environment'),
						},
						{
							key: 'startDate',
							render: (value) =>
								formatDate(value as string | undefined),
							title: i18n.translate('start-date'),
						},
						{
							key: 'endDate',
							render: (value) =>
								formatDate(value as string | undefined),
							title: i18n.translate('end-date'),
						},
					]}
					hasKebabButton
					hasPagination={totalCount > PAGE_SIZE}
					paginationProps={{
						activeDelta: PAGE_SIZE,
						activePage: page,
						onDeltaChange: () => {},
						onPageChange: setPage,
						totalItems: totalCount,
					}}
					rows={items}
				/>
			)}

			<Modal
				first={
					<ClayButton
						displayType="secondary"
						onClick={() => deleteModal.onOpenChange(false)}
					>
						{i18n.translate('cancel')}
					</ClayButton>
				}
				last={
					<ClayButton
						displayType="danger"
						onClick={handleConfirmDelete}
					>
						{i18n.translate('delete')}
					</ClayButton>
				}
				observer={deleteModal.observer}
				size="sm"
				status="warning"
				title={i18n.translate('delete')}
				visible={deleteModal.open}
			>
				{i18n.translate(
					'are-you-sure-you-want-to-delete-this-common-license-key'
				)}
			</Modal>
		</>
	);
}

export default function LicenseKeyUploads() {
	const [searchParams, setSearchParams] = useSearchParams();

	const activeTab: Tab =
		searchParams.get('tab') === 'elasticsearch'
			? 'elasticsearch'
			: 'commerce';

	function handleTabChange(tab: Tab) {
		setSearchParams((previousSearchParams) => {
			const nextSearchParams = new URLSearchParams(previousSearchParams);

			nextSearchParams.set('tab', tab);

			return nextSearchParams;
		});
	}

	return (
		<Page title={i18n.translate('license-key-uploads')}>
			<ClayTabs
				active={Math.max(0, TABS.indexOf(activeTab))}
				className="mb-4"
				onActiveChange={(index) => handleTabChange(TABS[index])}
			>
				{TABS.map((tab) => (
					<ClayTabs.Item key={tab}>
						{i18n.translate(tab)}
					</ClayTabs.Item>
				))}
			</ClayTabs>

			<LicenseKeyUploadsPanel
				key={activeTab}
				productGroup={PRODUCT_GROUP_BY_TAB[activeTab]}
			/>
		</Page>
	);
}
