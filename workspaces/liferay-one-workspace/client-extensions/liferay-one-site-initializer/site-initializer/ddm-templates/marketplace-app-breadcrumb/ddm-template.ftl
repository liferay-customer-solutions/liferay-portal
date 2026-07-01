<#assign appName = "" />

<#if (CPDefinition_name.getData())??>
	<#assign appName = CPDefinition_name.getData() />
</#if>

<nav aria-label="breadcrumb">
	<ol class="breadcrumb">
		<li class="breadcrumb-item">
			<a class="text-decoration-none text-secondary" href="/home">Home</a>
		</li>
		<li class="breadcrumb-item">
			<a class="text-decoration-none text-secondary" href="/marketplace">Marketplace</a>
		</li>
		<li class="breadcrumb-item">
			<a class="text-decoration-none text-secondary" href="/marketplace/applications">Applications Catalog</a>
		</li>
		<li class="active breadcrumb-item">
			${appName}
		</li>
	</ol>
</nav>