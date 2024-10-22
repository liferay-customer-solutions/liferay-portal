/**
 * SPDX-FileCopyrightText: (c) 2000 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

import {createContext, useContext, useEffect, useReducer} from 'react';
import IAccountBrief from '~/common/interfaces/accountBrief';
import IAccountSubscriptionGroup from '~/common/interfaces/accountSubscriptionGroup';
import IKoroneikiAccount from '~/common/interfaces/koroneikiAccount';
import IOrganizationBrief from '~/common/interfaces/organizationBrief';
import IProject from '~/common/interfaces/project';
import IUserAccount from '~/common/interfaces/userAccount';

import {useAppPropertiesContext} from '../../../common/contexts/AppPropertiesContext';
import {Liferay} from '../../../common/services/liferay';
import {
	addAccountFlag,
	getAccountSubscriptionGroups,
	getAnalyticsCloudWorkspace,
	getDXPCloudEnvironment,
	getKoroneikiAccounts,
	getLiferayExperienceCloudEnvironments,
	getUserAccount,
} from '../../../common/services/liferay/graphql/queries';
import {getCurrentSession} from '../../../common/services/okta/rest/getCurrentSession';
import {ROLE_TYPES, ROUTE_TYPES} from '../../../common/utils/constants';
import {getAccountKey} from '../../../common/utils/getAccountKey';
import {isValidPage} from '../../../common/utils/page.validation';
import {ONBOARDING_STEP_TYPES} from '../utils/constants';
import reducer, {actionTypes} from './reducer';

interface IOnboardingAction {
	payload: any;
	type: string;
}

interface IOnboardingState {
	analyticsCloudActivationSubmittedStatus: boolean | undefined;
	dxpCloudActivationSubmittedStatus: boolean | undefined;
	koroneikiAccount: IKoroneikiAccount;
	liferayExperienceCloudActivationSubmittedStatus: boolean | undefined;
	project: IProject | undefined;
	sessionId: string;
	step: number;
	subscriptionGroups: IAccountSubscriptionGroup[] | undefined;
	userAccount: IUserAccount | undefined;
}

interface IProps {
	children: any;
}

type OnboardingContext = [IOnboardingState, React.Dispatch<IOnboardingAction>];

const AppContext = createContext<OnboardingContext | null>(null);

const AppContextProvider: React.FC<IProps> = ({children}) => {
	const {client, oktaSessionAPI} = useAppPropertiesContext();
	const [state, dispatch] = useReducer<
		React.Reducer<IOnboardingState, IOnboardingAction>
	>(reducer, {
		analyticsCloudActivationSubmittedStatus: undefined,
		dxpCloudActivationSubmittedStatus: undefined,
		koroneikiAccount: {
			accountKey: '',
			dxpVersion: '',
			id: 0,
			name: '',
		} as IKoroneikiAccount,
		liferayExperienceCloudActivationSubmittedStatus: undefined,
		project: undefined,
		sessionId: '',
		step: ONBOARDING_STEP_TYPES.welcome,
		subscriptionGroups: undefined,
		userAccount: undefined,
	});

	useEffect(() => {
		const getUser = async (
			projectExternalReferenceCode: string
		): Promise<IUserAccount | undefined> => {
			const {data} = await client.query({
				query: getUserAccount,
				variables: {
					id: Liferay.ThemeDisplay.getUserId(),
				},
			});

			if (data) {
				const isAccountAdministrator = Boolean(
					data.userAccount?.accountBriefs
						?.find(
							({
								externalReferenceCode,
							}: {
								externalReferenceCode: string;
							}) =>
								externalReferenceCode ===
								projectExternalReferenceCode
						)
						?.roleBriefs?.find(
							({name}: {name: string}) =>
								name === ROLE_TYPES.admin.key
						)
				);

				const isAccountProvisioning = Boolean(
					data.userAccount?.accountBriefs
						?.find(
							({
								externalReferenceCode,
							}: {
								externalReferenceCode: string;
							}) =>
								externalReferenceCode ===
								projectExternalReferenceCode
						)
						?.roleBriefs?.find(
							({name}: {name: string}) => name === 'Provisioning'
						)
				);

				const isOmniAdmin = Boolean(
					data.userAccount?.roleBriefs?.find(
						({name}: {name: string}) => name === 'Administrator'
					)
				);

				const isStaff = data.userAccount?.organizationBriefs?.some(
					(organization: IOrganizationBrief) =>
						organization.name === 'Liferay Staff'
				);

				const userAccount: IUserAccount = {
					...data.userAccount,
					isAccountAdmin: isAccountAdministrator,
					isOmniAdmin,
					isProvisioning: isAccountProvisioning,
					isStaff,
				};

				dispatch({
					payload: userAccount,
					type: actionTypes.UPDATE_USER_ACCOUNT,
				});

				return userAccount;
			}

			return undefined;
		};

		const getProject = async (
			externalReferenceCode: string,
			accountBrief: IAccountBrief
		): Promise<void> => {
			const {data: projects} = await client.query({
				query: getKoroneikiAccounts,
				variables: {
					filter: `accountKey eq '${externalReferenceCode}'`,
				},
			});

			if (projects) {
				dispatch({
					payload: {
						...projects.c.koroneikiAccounts.items[0],
						id: accountBrief.id,
						name: accountBrief.name,
					},
					type: actionTypes.UPDATE_PROJECT,
				});
			}
		};

		const getSessionId = async (): Promise<void> => {
			const session = await getCurrentSession(oktaSessionAPI);

			if (session) {
				dispatch({
					payload: session.id,
					type: actionTypes.UPDATE_SESSION_ID,
				});
			}
		};

		const getSubscriptionGroups = async (
			accountKey: string
		): Promise<void> => {
			const {data} = await client.query({
				query: getAccountSubscriptionGroups,
				variables: {
					filter: `accountKey eq '${accountKey}' and hasActivation eq true`,
				},
			});

			if (data) {
				const items = data.c?.accountSubscriptionGroups?.items;
				dispatch({
					payload: items,
					type: actionTypes.UPDATE_SUBSCRIPTION_GROUPS,
				});
			}
		};

		const getDXPCloudActivationStatus = async (
			accountKey: string
		): Promise<void> => {
			const {data} = await client.query({
				query: getDXPCloudEnvironment,
				variables: {
					filter: `accountKey eq '${accountKey}'`,
					scopeKey: Liferay.ThemeDisplay.getScopeGroupId(),
				},
			});

			if (data) {
				const status = Boolean(
					data.c?.dXPCloudEnvironments?.items?.length
				);

				dispatch({
					payload: status,
					type: actionTypes.UPDATE_DXP_CLOUD_ACTIVATION_SUBMITTED_STATUS,
				});
			}
		};

		const getAnalyticsCloudActivationStatus = async (
			accountKey: string
		): Promise<void> => {
			const {data} = await client.query({
				query: getAnalyticsCloudWorkspace,
				variables: {
					filter: `accountKey eq '${accountKey}'`,
					scopeKey: Liferay.ThemeDisplay.getScopeGroupId(),
				},
			});

			if (data) {
				const status = Boolean(
					data.c?.analyticsCloudWorkspaces?.items?.length
				);

				dispatch({
					payload: status,
					type: actionTypes.UPDATE_ANALYTICS_CLOUD_ACTIVATION_SUBMITTED_STATUS,
				});
			}
		};

		const getLiferayExperienceCloudActivationStatus = async (
			accountKey: string
		): Promise<void> => {
			const {data} = await client.query({
				query: getLiferayExperienceCloudEnvironments,
				variables: {
					filter: `accountKey eq '${accountKey}'`,
				},
			});

			if (data) {
				const status = Boolean(
					data.c?.liferayExperienceCloudEnvironments?.items?.length
				);

				dispatch({
					payload: status,
					type: actionTypes.UPDATE_LIFERAY_EXPERIENCE_CLOUD_ACTIVATION_SUBMITTED_STATUS,
				});
			}
		};

		const fetchData = async (): Promise<void> => {
			const projectExternalReferenceCode = getAccountKey();

			const user = await getUser(projectExternalReferenceCode);

			if (!user) {
				return;
			}

			const isValid = await isValidPage(
				client,
				user,
				projectExternalReferenceCode,
				ROUTE_TYPES.onboarding
			);

			if (user && isValid) {
				const accountBrief = user.accountBriefs?.find(
					(accountBrief) =>
						accountBrief?.externalReferenceCode ===
						projectExternalReferenceCode
				) as IAccountBrief;

				if (accountBrief) {
					getProject(projectExternalReferenceCode, accountBrief);
					getSubscriptionGroups(projectExternalReferenceCode);
					getDXPCloudActivationStatus(projectExternalReferenceCode);
					getAnalyticsCloudActivationStatus(
						projectExternalReferenceCode
					);
					getLiferayExperienceCloudActivationStatus(
						projectExternalReferenceCode
					);
					getSessionId();

					client.mutate({
						context: {
							displaySuccess: false,
							type: 'liferay-rest',
						},
						mutation: addAccountFlag,
						variables: {
							accountFlag: {
								accountEntryId: state.project?.id,
								accountKey: projectExternalReferenceCode,
								finished: true,
								name: ROUTE_TYPES.onboarding,
								r_accountEntryToAccountFlag_accountEntryId:
									accountBrief.id,
							},
						},
					});
				}
			}
		};

		fetchData();
	}, [client, oktaSessionAPI, state.project?.id]);

	return (
		<AppContext.Provider value={[state, dispatch]}>
			{children}
		</AppContext.Provider>
	);
};

const useOnboarding = (): OnboardingContext =>
	useContext(AppContext) as OnboardingContext;

export {AppContext, AppContextProvider, useOnboarding};
