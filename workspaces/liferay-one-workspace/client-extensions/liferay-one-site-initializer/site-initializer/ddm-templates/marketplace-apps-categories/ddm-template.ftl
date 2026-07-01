<#if themeDisplay?has_content>
	<#assign scopeGroupId = themeDisplay.getScopeGroupId() />
</#if>

<#if currentURL?has_content>
	<#if currentURL?contains('web')>
		<#assign
			index = 2
			partsUrl = currentURL?split('/')
			siteName = partsUrl[index..index]?join('/')
		/>
	</#if>
</#if>

<#assign channel = restClient.get("/headless-commerce-delivery-catalog/v1.0/channels?accountId=-1&filter=siteGroupId eq '${scopeGroupId}'") />

<#if channel?has_content>
	<#assign channelId = channel.items[0].id />
</#if>

<#if (CPDefinition_cProductId.getData())??>
	<#assign productId = CPDefinition_cProductId.getData() />
</#if>

<#assign
	product = restClient.get("/headless-commerce-delivery-catalog/v1.0/channels/"+ channelId +"/products/"+ productId +"?accountId=-1&nestedFields=categories")

	categories = product.categories![]

	marketplaceAppCategories = categories?filter(category -> stringUtil.equals(category.vocabulary?upper_case?replace(" ", "-", "r"), "MARKETPLACE-APP-CATEGORY"))
	marketplaceCategory = categories?filter(category -> stringUtil.equals(category.vocabulary?upper_case?replace(" ", "-", "r"), "MARKETPLACE-CATEGORY"))?first!""
/>

<div class="app-container d-flex flex-wrap font-size-paragraph-small justify-content-between w-100">
	<div class="align-items-center app-details-category-badge d-flex">
		<#if marketplaceCategory?has_content>
			<#assign
				badgeType = marketplaceCategory.name?lower_case?replace(" ", "-", "r")?replace("/", "-", "r")
				marketplaceCategoryTitle = languageUtil.get(locale, marketplaceCategory.name, marketplaceCategory.title)
			/>

			<#if stringUtil.equals(marketplaceCategory.name, "other")>
				<div></div>
			<#else>
				<a class="text-decoration-none text-reset" href="/applications?marketplaceCategory=${marketplaceCategory.name?url('UTF-8')}">
					<span class="align-items-center app-type-badge border-radius-small d-flex mr-2 px-3 rounded ${badgeType}" title="${marketplaceCategoryTitle}">
						${marketplaceCategoryTitle}
					</span>
				</a>
			</#if>
		</#if>

		<#if marketplaceAppCategories?has_content>
			<#if marketplaceCategory?has_content && !stringUtil.equals(marketplaceCategory.name, "other")>
				<span class="align-items-center d-flex justify-content-between">
					<span class="align-items-center d-flex diamond-icon-container justify-content-between mr-3">
						<@clay["icon"] symbol="diamond" />
					</span>
				</span>
			</#if>

			<#list marketplaceAppCategories as marketplaceAppCategory>
				<#assign marketplaceAppCategoryTitle = languageUtil.get(locale, marketplaceAppCategory.name, marketplaceAppCategory.title) />

				<a class="text-decoration-none text-reset" href="/applications?marketplaceAppCategory=${marketplaceAppCategory.name?url('UTF-8')}">
					<span class="font-weight-normal mr-2 product-tag px-2 py-1 rounded text-nowrap" title="${marketplaceAppCategoryTitle}">
						${marketplaceAppCategoryTitle}
					</span>
				</a>
			</#list>
		</#if>
	</div>
</div>