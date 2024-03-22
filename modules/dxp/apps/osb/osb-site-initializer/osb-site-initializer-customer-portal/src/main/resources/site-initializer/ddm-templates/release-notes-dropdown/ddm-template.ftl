<#if themeDisplay?has_content && (AssetCategory_vocabulary.getData())??>
	<#assign quarterlyReleaseVocabularyId = (request.getAttribute("INFO_ITEM").vocabularyId)! />

	<#if quarterlyReleaseVocabularyId?has_content>
		<#assign releaseCategories = (restClient.get("/headless-admin-taxonomy/v1.0/taxonomy-vocabularies/${quarterlyReleaseVocabularyId}/taxonomy-categories?pageSize=4&sort=dateCreated:desc").items)! />
	</#if>
</#if>

<style>
	#dropdownReleaseNotes {
		font-size: var(--h2-font-size, 1.375rem) !important;
		font-weight: var(--h2-font-weight) !important;
	}
</style>

<div class="dropdown">
	<button
		aria-expanded="false"
		aria-haspopup="true"
		class="btn dropdown-toggle p-0 text-neutral-0"
		data-toggle="liferay-dropdown"
		id="dropdownReleaseNotes"
		type="button"
	>
		<#if (AssetCategory_name.getData())??>
			${AssetCategory_name.getData()}
		</#if>

		<@clay["icon"] symbol="caret-bottom" />
	</button>

	<ul
		aria-labelledby="dropdownReleaseNotes"
		class="dropdown-menu"
		style="top: 0px; transform: translate3d(0px, 40px, 0px);"
		x-placement="bottom-start"
	>
		<#if releaseCategories?has_content>
			<#list releaseCategories as releaseCategory>
				<#assign friendlyURL = (releaseCategory.taxonomyCategoryProperties?filter(taxonomyCategoryProperty -> stringUtil.equals(taxonomyCategoryProperty.key, "friendlyURL"))?first.value)! />

				<li>
					<div class="dropdown-item" onclick="buttonTab('${releaseCategory.id}?r=${releaseCategory.id}')">
						${releaseCategory.name}

						<#if (AssetCategory_name.getData())?? && releaseCategory.name == AssetCategory_name.getData()>
							<span class="dropdown-item-indicator-end">
								<@clay["icon"] symbol="check" />
							</span>
						</#if>
					</div>
				</li>
			</#list>

			<li>
				<a class="dropdown-item" href="#1">
					Previous Release

					<span class="dropdown-item-indicator-end">
						<@clay["icon"] symbol="shortcut" />
					</span>
				</a>
			</li>
		</#if>
	</ul>
</div>

<script>
	const tabs = {
		'tab1': 'RELEASE-NOTES-HIGHLIGHT-STRUCTURE',
		'tab2': 'RELEASE-NOTES-FEATURE-STRUCTURE',
		'tab3': 'RELEASE-NOTES-BREAKING-CHANGE-STRUCTURE',
	};

	function getValueForElementId(elementId) {
		return tabs[elementId] || null;
	}

	function addURL(value) {
		let currentURL = window.location.href;
		const index = currentURL.indexOf('v/');

		if (index !== -1) {
			currentURL = currentURL.substring(0, index);
		}

		const newURL = currentURL + 'v/' + value;

		window.location.href = newURL;
	}

	let value = 'RELEASE-NOTES-HIGHLIGHT-STRUCTURE';

	document.addEventListener('DOMContentLoaded', function() {
		document.querySelectorAll('.active').forEach(function(element) {
				value = getValueForElementId(element.id);
		});
	});

	function buttonTab(previousURL) {
		let newValue = previousURL + '&t=' + value;

		addURL(newValue);
	}
</script>