<#--
Renders the site navigation menu the tag supplies as `entries`. Liferay filters
that tree by each menu item's own permissions and resolves every link, so an
item standing for a page the current user may not view never reaches this
template and there is no page visibility logic here. An item is a dropdown
parent when it has children. The headless API supplies only the presentation
custom fields, which it resolves for the current locale.
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

<#function getCustomFields navItem>
	<#-- On a site navigation menu item, getLayoutId() is the menu item id. -->

	<#return (customFieldsMaps[navItem.getLayoutId()?c])!{} />
</#function>

<#assign
	canBypassMyAccount = false
	customFieldsMaps = {}
	selectedSectionPlid = 0
/>

<#--
The header fragment's JavaScript reads this to decide whether to hide the account
menu from a user who has no account of their own. It is the one rule here that a
page permission cannot carry.
-->

<#attempt>
	<#list themeDisplay.getUser().getRoles() as userRole>
		<#if stringUtil.equals(userRole.getName(), "Administrator") || stringUtil.equals(userRole.getName(), "Liferay Staff")>
			<#assign canBypassMyAccount = true />
		</#if>
	</#list>
<#recover>
	<#assign canBypassMyAccount = false />
</#attempt>

<#attempt>
	<#assign navigationMenu = restClient.get("/headless-delivery/v1.0/sites/" + themeDisplay.getScopeGroupId()?c + "/navigation-menus/by-external-reference-code/LO_PRIMARY_NAV?nestedFields=customFields,navigationMenuItems") />

	<#assign customFieldsMaps = getCustomFieldsMaps((navigationMenu.navigationMenuItems)![]) />
<#recover>
	<#assign customFieldsMaps = {} />
</#attempt>

<#-- A section stays selected anywhere below its top level page. -->

<#attempt>
	<#assign selectedSectionPlid = themeDisplay.getLayout().getAncestorPlid() />
<#recover>
	<#assign selectedSectionPlid = 0 />
</#attempt>

<ul class="adt-navigation" data-account-bypass="${canBypassMyAccount?c}">
	<#attempt>
		<#list entries![] as navPrimaryItem>
			<#assign
				navPrimaryItemChildren = navPrimaryItem.getChildren()
				navPrimaryItemSelected = (selectedSectionPlid > 0) && (((navPrimaryItem.getLayout().getPlid())!0) == selectedSectionPlid)
			/>

			<#if navPrimaryItemChildren?has_content>
				<div class="adt-nav-item dropdown dropdown-action<#if navPrimaryItemSelected> selected</#if> w-100">
					<button
						aria-expanded="false"
						class="adt-nav-text align-items-center d-flex menu-info"
						data-toggle="liferay-dropdown"
						tabindex="4"
					>
						<span class="adt-nav-title text-truncate">
							${navPrimaryItem.getName()}
						</span>
						<span class="adt-nav-caret-bottom-icon align-self-center">
							<svg class="lexicon-icon lexicon-icon-caret-bottom" role="presentation" viewBox="0 0 512 512"><use xlink:href="/o/admin-theme/images/clay/icons.svg#caret-bottom"></use></svg>
						</span>
					</button>

					<@renderNavigationDropdown navPrimaryItemChildren />
				</div>
			<#else>
				<a class="adt-nav-item<#if navPrimaryItemSelected> selected</#if> w-100" href="${navPrimaryItem.getRegularURL()}" ${navPrimaryItem.getTarget()}>
					<div class="adt-nav-text d-flex pr-3" tabindex="4">
						<span class="adt-nav-title text-truncate">
							${navPrimaryItem.getName()}
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
					<#assign navSecondaryItemCustomFields = getCustomFields(navSecondaryItem) />

					<#assign
						navSecondaryItemChildColumns = (navSecondaryItemCustomFields["Submenu Child Columns"])!""
						navSecondaryItemColumnSpan = (navSecondaryItemCustomFields["Submenu Column Span"])!""
						navSecondaryItemImageURL = (navSecondaryItemCustomFields["Menu Item Image URL"])!""
						navSecondaryItemType = (navSecondaryItemCustomFields["Menu Item Type"])!""
						submenuBackground = (navSecondaryItemCustomFields["Submenu Background"])!""
					/>

					<#assign
						childColumnSpan = ""
						sectionColumnSpan = ""
					/>

					<#if navSecondaryItemColumnSpan?has_content>
						<#assign sectionColumnSpan = "_" + navSecondaryItemColumnSpan + "-section-span" />

						<#if navSecondaryItemChildColumns?has_content>
							<#assign childColumnSpan = (navSecondaryItemColumnSpan?number / navSecondaryItemChildColumns?number)?floor?string />
						</#if>
					</#if>

					<ul class="adt-submenu-section ${submenuBackground} ${sectionColumnSpan}">
						<li class="adt-submenu-header color-neutral-8 font-size-small-caps">
							<#if stringUtil.equals(navSecondaryItemType, "Image") && navSecondaryItemImageURL?has_content>
								<img class="adt-submenu-header-image" loading="lazy" src="${navSecondaryItemImageURL}" />
							</#if>
							${navSecondaryItem.getName()}
						</li>

						<#list navSecondaryItem.getChildren() as navTertiaryItem>
							<#assign navTertiaryItemCustomFields = getCustomFields(navTertiaryItem) />

							<#assign
								navTertiaryItemDescription = (navTertiaryItemCustomFields["Menu Item Description"])!""
								navTertiaryItemImageURL = (navTertiaryItemCustomFields["Menu Item Image URL"])!""
								navTertiaryItemName = navTertiaryItem.getName()
								navTertiaryItemPreheader = (navTertiaryItemCustomFields["Menu Item Preheader"])!""
								navTertiaryItemType = (navTertiaryItemCustomFields["Menu Item Type"])!""
							/>

							<li class="adt-submenu-item-content ${navTertiaryItemType?lower_case}-type grid-column-span-${childColumnSpan}">
								<a class="adt-submenu-item-link" href="${navTertiaryItem.getRegularURL()}" ${navTertiaryItem.getTarget()} tabindex="4">
									<#if stringUtil.equals(navTertiaryItemType, "Image") && navTertiaryItemImageURL?has_content>
										<img class="adt-submenu-item-image" loading="lazy" src="${navTertiaryItemImageURL}" />
									</#if>

									<div class="adt-submenu-item-text">
										<#if stringUtil.equals(navTertiaryItemType, "Image") && navTertiaryItemPreheader?has_content>
											<div class="adt-submenu-item-preheader color-neutral-3 font-weight-semi-bold">
												${navTertiaryItemPreheader}
											</div>
										</#if>

										<div class="adt-submenu-item-title h5" data-nav-name="${navTertiaryItemName}">
											${navTertiaryItemName}
										</div>

										<#if navTertiaryItemDescription?has_content>
											<div class="adt-submenu-item-description">
												${navTertiaryItemDescription}
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
