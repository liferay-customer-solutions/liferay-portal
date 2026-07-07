<#--
	Vertical sidebar for the One Liferay Help section. Self-fetches the
	"Support Navigation" menu (LO_SUPPORT_NAV) by external reference code, the
	same pattern the primary and footer navigation templates use, so it renders
	the curated help topics regardless of which menu the portlet has selected.
-->

<#assign
	currentURL = ""
	layoutFriendlyURL = ""
/>

<#attempt>
	<#assign currentURL = themeDisplay.getURLCurrent() />
<#recover>
	<#assign currentURL = "" />
</#attempt>

<#attempt>
	<#assign layoutFriendlyURL = themeDisplay.getLayout().getFriendlyURL() />
<#recover>
	<#assign layoutFriendlyURL = "" />
</#attempt>

<#attempt>
	<#assign navigationMenu = restClient.get("/headless-delivery/v1.0/sites/" + themeDisplay.getScopeGroupId()?c + "/navigation-menus/by-external-reference-code/LO_SUPPORT_NAV?nestedFields=navigationMenuItems") />
<#recover>
	<#assign navigationMenu = {} />
</#attempt>

<#assign
	navigationIcons = {
		"Activation": "cog",
		"Administration": "users",
		"Getting Started": "analytics",
		"Team Members": "list",
		"Using One Liferay": "document-text"
	}
/>

<nav aria-label="One Liferay Help" class="lo-support-nav">
	<ul class="list-unstyled mb-0">
		<#if (navigationMenu.navigationMenuItems)??>
			<#list navigationMenu.navigationMenuItems as navigationMenuItem>
				<#assign
					navigationMenuItemURL = (navigationMenuItem.typeSettings.url)!"#"

					navigationIconSymbol = (navigationIcons[navigationMenuItem.name])!"circle"
					navigationMenuItemActive = navigationMenuItemURL?has_content && (navigationMenuItemURL != "#") && ((currentURL?has_content && currentURL?contains(navigationMenuItemURL)) || (layoutFriendlyURL?has_content && (navigationMenuItemURL == layoutFriendlyURL || navigationMenuItemURL?ends_with(layoutFriendlyURL))))
				/>

				<li>
					<a
						class="align-items-center d-flex lo-support-nav-link<#if navigationMenuItemActive> active</#if>"
						href="${navigationMenuItemURL}"
						<#if stringUtil.equals((navigationMenuItem.typeSettings.useNewTab)!"", "true")>target="_blank"</#if>
					>
						<span class="align-items-center d-flex lo-support-nav-icon">
							<svg class="lexicon-icon" role="presentation" viewBox="0 0 512 512"><use xlink:href="/o/admin-theme/images/clay/icons.svg#${navigationIconSymbol}" /></svg>
						</span>
						<span class="lo-support-nav-label">${navigationMenuItem.name}</span>
					</a>
				</li>
			</#list>
		</#if>
	</ul>
</nav>

<style>
	.lo-support-nav {
		background-color: var(--color-neutral-1, #f7f8f9);
		border-radius: 12px;
		padding: 0.75rem;
	}

	.lo-support-nav-link {
		border-radius: 8px;
		color: var(--color-neutral-9, #272833);
		gap: 0.75rem;
		padding: 0.625rem 0.875rem;
		text-decoration: none;
		transition: background-color 0.1s ease-in-out, color 0.1s ease-in-out;
	}

	.lo-support-nav-link + .lo-support-nav-link,
	.lo-support-nav li + li .lo-support-nav-link {
		margin-top: 0.25rem;
	}

	.lo-support-nav-link:focus,
	.lo-support-nav-link:hover {
		background-color: var(--color-neutral-2, #e7e7ed);
		color: var(--color-neutral-9, #272833);
		text-decoration: none;
	}

	.lo-support-nav-link.active {
		background-color: rgba(11, 95, 255, 0.1);
		color: var(--color-brand-primary, #0b5fff);
		font-weight: var(--font-weight-semi-bold, 600);
	}

	.lo-support-nav-icon {
		color: var(--color-neutral-6, #6b6c7e);
		flex-shrink: 0;
	}

	.lo-support-nav-link.active .lo-support-nav-icon {
		color: var(--color-brand-primary, #0b5fff);
	}

	.lo-support-nav-icon .lexicon-icon {
		height: 1rem;
		margin-top: 0;
		width: 1rem;
	}
</style>