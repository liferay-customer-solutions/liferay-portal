<#assign
	scopeGroupId = themeDisplay.getScopeGroupId()
	specificationGroups = cpContentHelper.getCPOptionCategories(themeDisplay.getCompanyId())

	channelId = ""
/>

<#attempt>
	<#assign channel = restClient.get("/headless-commerce-delivery-catalog/v1.0/channels?accountId=-1&filter=siteGroupId eq '${scopeGroupId}'") />

	<#if (channel.items)?has_content>
		<#assign channelId = channel.items[0].id />
	</#if>
<#recover>
</#attempt>

<#function getSpecificationValue specificationGroupKey specificationKey productId defaultValue="">
	<#local specificationGroup = specificationGroups?filter(specificationGroup -> specificationGroup.getKey() == specificationGroupKey) />

	<#if specificationGroup?has_content>
		<#local specifications = cpContentHelper.getCategorizedCPDefinitionSpecificationOptionValues(productId, specificationGroup?first.getCPOptionCategoryId()) />

		<#local specification = specifications?filter(productSpecification ->
			stringUtil.equals(productSpecification.getCPSpecificationOption().getKey(), specificationKey)) />

		<#return (specification?first.getValue(locale))!defaultValue />
	</#if>

	<#return defaultValue />
</#function>

<#function langKey label>
	<#return label?lower_case?replace(" ", "-", "r")?replace("&", "and", "r")?replace(",", "", "r")?replace("/", "-", "r") />
</#function>

<div class="card-grid">
	<div class="cards-container">
		<#if entries?has_content>
			<#list entries as entry>
				<#if entry?has_content>
					<#assign
						developerName = getSpecificationValue("product-metadata", "developer-name", entry.getCPDefinitionId())
						priceModel = getSpecificationValue("pricing-licensing-terms", "price-model", entry.getCPDefinitionId())
						productDescription = stringUtil.shorten(htmlUtil.stripHtml(entry.getDescription()!""), 120, "...")
						productId = entry.getCPDefinitionId()
						productImage = cpContentHelper.getDefaultImageFileURL(-1, entry.getCPDefinitionId())

						categoriesListSize = 0
						principalCategory = ""
						productCategories = []
						remainingCategoriesText = []
					/>

					<#if channelId?has_content>
						<#attempt>
							<#assign product = restClient.get("/headless-commerce-delivery-catalog/v1.0/channels/" + channelId + "/products/" + entry.getCProductId() + "?accountId=-1&nestedFields=categories") />

							<#if (product.categories)?has_content>
								<#assign productCategories = product.categories?filter(productCategory -> productCategory.vocabulary?replace(" ", "-") == "marketplace-solution-category") />
							</#if>
						<#recover>
						</#attempt>
					</#if>

					<#if productCategories?has_content>
						<#assign
							principalCategory = productCategories[0]
							categoriesListSize = productCategories?size - 1
							remainingCategories = productCategories?filter(category -> category.name != principalCategory.name)
						/>

						<#list remainingCategories as category>
							<#assign remainingCategoriesText = remainingCategoriesText + [languageUtil.get(locale, langKey(category.title), category.title)] />
						</#list>
					</#if>

					<a class="app-card d-flex flex-column one-card overflow-hidden text-dark text-decoration-none" href="${cpContentHelper.getFriendlyURL(entry, themeDisplay)}">
						<div class="align-items-center d-flex overflow-hidden solution-card-image-container">
							<img alt="${entry.getName()}" class="solution-card-image w-100" draggable="false" loading="lazy" src="${productImage}" />
						</div>

						<div class="d-flex flex-column h-100 justify-content-between p-4">
							<div class="d-flex flex-column">
								<#if developerName?has_content>
									<p class="catalog-name font-weight-normal m-0 text-black-50">${developerName}</p>
								</#if>

								<h3 class="app-name font-weight-semi-bold mb-1 mt-1">${entry.getName()}</h3>

								<#if productDescription?has_content>
									<p class="app-card-description font-weight-normal">${productDescription}</p>
								</#if>
							</div>

							<div class="d-flex flex-column">
								<#if priceModel?has_content>
									<span class="card-price mt-auto">
										${priceModel?cap_first}
									</span>
								</#if>

								<#if principalCategory?has_content>
									<#assign
										principalCategoryTitle = languageUtil.get(locale, langKey(principalCategory.title), principalCategory.title)
										tagName = stringUtil.shorten(htmlUtil.stripHtml(principalCategoryTitle!""), 100, "...")
									/>

									<div class="my-1 tags-container">
										<span class="font-weight-normal mb-3 mr-3 product-tag px-2 py-1 rounded text-nowrap" title="${principalCategoryTitle}">
											${tagName}
										</span>

										<#if categoriesListSize gt 0 && remainingCategoriesText?has_content>
											<span class="font-weight-normal mb-1 product-tag px-2 py-1 rounded text-nowrap" title="${remainingCategoriesText?join('\n')}">
												+ ${categoriesListSize}
											</span>
										</#if>
									</div>
								</#if>
							</div>
						</div>
					</a>
				</#if>
			</#list>
		</#if>
	</div>
</div>