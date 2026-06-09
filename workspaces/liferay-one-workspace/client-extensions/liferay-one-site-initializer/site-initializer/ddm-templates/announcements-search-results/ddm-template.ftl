<div class="lo-announcements-results">
	<div class="lo-announcements-results-count">
		<strong>${searchContainer.getTotal()}</strong> ${languageUtil.get(locale, "announcements")}
	</div>

	<ul class="list-unstyled lo-announcements-list">
		<#if entries?has_content>
			<#list entries as entry>
				<li class="lo-announcement-card">
					<a class="lo-announcement-card-title" href="${entry.getViewURL()}">
						${entry.getHighlightedTitle()}
					</a>

					<#if entry.isContentVisible()>
						<p class="lo-announcement-card-content">
							${entry.getContent()}
						</p>
					</#if>

					<div class="lo-announcement-card-meta">
						<#if entry.isCreationDateVisible()>
							<span class="lo-announcement-card-date">
								${entry.getCreationDateString()}
							</span>
						</#if>

						<#if entry.isAssetCategoriesOrTagsVisible()>
							<span class="lo-announcement-card-categories">
								<@liferay_asset["asset-categories-summary"]
									className=entry.getClassName()
									classPK=entry.getClassPK()
									paramName=entry.getFieldAssetCategoryIds()
									portletURL=entry.getPortletURL()
								/>
							</span>
						</#if>
					</div>
				</li>
			</#list>
		</#if>
	</ul>
</div>

<style>
	.lo-announcements-results .lo-announcements-results-count {
		color: #54555f;
		margin-bottom: 16px;
		text-align: right;
	}

	.lo-announcements-results .lo-announcement-card {
		background-color: #fff;
		border: 1px solid #e7e7ed;
		border-radius: 12px;
		margin-bottom: 16px;
		padding: 24px;
	}

	.lo-announcements-results .lo-announcement-card-title {
		color: #282934;
		display: block;
		font-size: 18px;
		font-weight: 600;
		margin-bottom: 8px;
		text-decoration: none;
	}

	.lo-announcements-results .lo-announcement-card-title:hover {
		text-decoration: underline;
	}

	.lo-announcements-results .lo-announcement-card-content {
		color: #54555f;
		display: -webkit-box;
		line-clamp: 2;
		margin-bottom: 16px;
		overflow: hidden;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: 2;
	}

	.lo-announcements-results .lo-announcement-card-meta {
		align-items: center;
		color: #6c6c75;
		display: flex;
		flex-wrap: wrap;
		font-size: 13px;
		gap: 12px;
	}

	.lo-announcements-results .lo-announcement-card-categories .asset-category {
		margin-right: 4px;
	}
</style>