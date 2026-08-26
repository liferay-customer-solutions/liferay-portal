<#--
Renders the site navigation menu the tag supplies as `entries`. Liferay filters
that tree by each menu item's own permissions, so an item pointing at a page the
current user may not view never reaches this template: there is no page
visibility logic here, and every link comes back resolved by Liferay. An item is
a dropdown parent when it has children. Only the presentation metadata comes from
the headless API, which resolves the custom fields for the current locale.
-->

<#function getCustomFieldsMaps navigationMenuItems>
	<#local customFieldsMaps = {} />

	<#list navigationMenuItems as navigationMenuItem>
		<#local
			customFieldsMap = {}
			navigationMenuItemId = (navigationMenuItem.id)!0
		/>

		<#list (navigationMenuItem.customFields)![] as customField>
			<#local data = (customField.customValue.data)!"" />

			<#if data?is_sequence>
				<#local data = (data?first)!"" />
			</#if>

			<#local customFieldsMap = customFieldsMap + {(customField.name)!"": data} />
		</#list>

		<#local customFieldsMaps = customFieldsMaps + {navigationMenuItemId?c: customFieldsMap} + getCustomFieldsMaps((navigationMenuItem.navigationMenuItems)![]) />
	</#list>

	<#return customFieldsMaps />
</#function>

<#function getCustomFieldsMap navItem>
	<#-- On a site navigation menu item, getLayoutId() is the menu item id. -->

	<#return (customFieldsMaps[navItem.getLayoutId()?c])!{} />
</#function>

<#assign
	canBypassMyAccount = false
	customFieldsMaps = {}
/>

<#--
The one rule the theme cannot read off a page: the header fragment's JavaScript
reads this to decide whether to hide the account menu from a user who has no
account of their own.
-->

<#attempt>
	<#if themeDisplay.isSignedIn()>
		<#list themeDisplay.getUser().getRoles() as userRole>
			<#assign userRoleName = userRole.getName() />

			<#if stringUtil.equals(userRoleName, "Administrator") || stringUtil.equals(userRoleName, "Liferay Staff")>
				<#assign canBypassMyAccount = true />
			</#if>
		</#list>
	</#if>
<#recover>
	<#assign canBypassMyAccount = false />
</#attempt>

<#attempt>
	<#assign navigationMenu = restClient.get("/headless-delivery/v1.0/sites/" + themeDisplay.getScopeGroupId()?c + "/navigation-menus/by-external-reference-code/LO_PRIMARY_NAV?nestedFields=customFields,navigationMenuItems") />

	<#assign customFieldsMaps = getCustomFieldsMaps((navigationMenu.navigationMenuItems)![]) />
<#recover>
	<#assign customFieldsMaps = {} />
</#attempt>

<#-- A section stays highlighted anywhere below its top level page. -->

<#assign activeLayoutPlid = 0 />

<#attempt>
	<#assign activeLayout = themeDisplay.getLayout() />

	<#list activeLayout.getAncestors() as ancestorLayout>
		<#assign activeLayout = ancestorLayout />
	</#list>

	<#assign activeLayoutPlid = activeLayout.getPlid() />
<#recover>
	<#assign activeLayoutPlid = 0 />
</#attempt>

<ul class="adt-navigation" data-account-bypass="${canBypassMyAccount?c}">
	<#attempt>
		<#list entries![] as navPrimaryItem>
			<#assign
				navPrimaryItemChildren = navPrimaryItem.getChildren()
				navPrimaryItemId = navPrimaryItem.getLayoutId()
				navPrimaryItemName = navPrimaryItem.getName()
				navPrimaryItemPlid = (navPrimaryItem.getLayout().getPlid())!0
			/>

			<#assign isActiveSection = (activeLayoutPlid > 0) && (navPrimaryItemPlid == activeLayoutPlid) />

			<#if navPrimaryItemChildren?has_content>
				<div class="adt-nav-item dropdown dropdown-action<#if isActiveSection> selected</#if> w-100">
					<button
						aria-expanded="false"
						class="adt-nav-text align-items-center d-flex menu-info"
						data-toggle="liferay-dropdown"
						id="main-menu-${navPrimaryItemId?c}"
						tabindex="4"
					>
						<span class="adt-nav-title text-truncate">
							${navPrimaryItemName}
						</span>
						<span class="adt-nav-caret-bottom-icon align-self-center">
							<svg class="lexicon-icon lexicon-icon-caret-bottom" role="presentation" viewBox="0 0 512 512"><use xlink:href="/o/admin-theme/images/clay/icons.svg#caret-bottom"></use></svg>
						</span>
					</button>

					<@renderNavigationDropdown navPrimaryItemChildren />
				</div>
			<#else>
				<a class="adt-nav-item<#if isActiveSection> selected</#if> w-100" href="${navPrimaryItem.getRegularURL()}"<#if ((navPrimaryItem.getTarget())!"")?contains("_blank")> target="_blank"</#if>>
					<div class="adt-nav-text d-flex pr-3" tabindex="4">
						<span class="adt-nav-title text-truncate">
							${navPrimaryItemName}
						</span>
					</div>
				</a>
			</#if>
		</#list>
	<#recover>
	</#attempt>
</ul>

<#macro renderNavigationDropdown
	navSecondaryItems
>
	<div class="adt-submenu dropdown-menu main-menu-dropdown position-absolute pt-2">
		<div class="adt-submenu-outer-wrapper container-fluid-max-xl">
			<div class="adt-submenu-inner-wrapper">
				<#list navSecondaryItems as navSecondaryItem>
					<#assign
						navSecondaryItemChildren = navSecondaryItem.getChildren()
						navSecondaryItemCustomFieldsMap = getCustomFieldsMap(navSecondaryItem)
					/>

					<#assign
						backgroundColor = (navSecondaryItemCustomFieldsMap["Submenu Background"])!""
						childColumns = (navSecondaryItemCustomFieldsMap["Submenu Child Columns"])!""
						columnSpan = (navSecondaryItemCustomFieldsMap["Submenu Column Span"])!""
						imageURL = (navSecondaryItemCustomFieldsMap["Menu Item Image URL"])!""
						menuItemType = (navSecondaryItemCustomFieldsMap["Menu Item Type"])!""
					/>

					<#if childColumns?has_content>
						<#assign childColumns = (columnSpan?number / childColumns?number)?floor?string />
					</#if>

					<#if columnSpan?has_content>
						<#assign columnSpan = "_" + columnSpan + "-section-span" />
					</#if>

					<ul class="adt-submenu-section ${backgroundColor} ${columnSpan}">
						<li class="adt-submenu-header color-neutral-8 font-size-small-caps">
							<#if stringUtil.equals(menuItemType, "Image") && imageURL?has_content>
								<img class="adt-submenu-header-image" loading="lazy" src="${imageURL}" />
							</#if>
							${navSecondaryItem.getName()}
						</li>

						<#list navSecondaryItemChildren as navTertiaryItem>
							<#assign
								navTertiaryItemCustomFieldsMap = getCustomFieldsMap(navTertiaryItem)
								navTertiaryItemName = navTertiaryItem.getName()
							/>

							<#assign
								descriptionText = (navTertiaryItemCustomFieldsMap["Menu Item Description"])!""
								imageURL = (navTertiaryItemCustomFieldsMap["Menu Item Image URL"])!""
								menuItemType = (navTertiaryItemCustomFieldsMap["Menu Item Type"])!""
								preheaderText = (navTertiaryItemCustomFieldsMap["Menu Item Preheader"])!""
							/>

							<li class="adt-submenu-item-content ${menuItemType?lower_case}-type grid-column-span-${childColumns}">
								<a class="adt-submenu-item-link" href="${navTertiaryItem.getRegularURL()}"<#if ((navTertiaryItem.getTarget())!"")?contains("_blank")> target="_blank"</#if> tabindex="4">
									<#if stringUtil.equals(menuItemType, "Image") && imageURL?has_content>
										<img class="adt-submenu-item-image" loading="lazy" src="${imageURL}" />
									</#if>

									<div class="adt-submenu-item-text">
										<#if stringUtil.equals(menuItemType, "Image") && preheaderText?has_content>
											<div class="adt-submenu-item-preheader color-neutral-3 font-weight-semi-bold">
												${preheaderText}
											</div>
										</#if>

										<div class="adt-submenu-item-title h5" data-nav-name="${navTertiaryItemName}">
											${navTertiaryItemName}
										</div>

										<#if descriptionText?has_content>
											<div class="adt-submenu-item-description">
												${descriptionText}
											</div>
										</#if>
									</div>
								</a>
							</li>
						</#list>
					</ul>
				</#list>
			</div>
		</div>
	</div>
</#macro>
