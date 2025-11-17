#!/bin/bash

CLUSTER_NAME="cne"
DOMAIN_SUFFIX="localtest.me"
LIFERAY_CLIENT_EXTENSION_HELM_URL="https://liferay-customer-solutions.github.io/liferay-portal/pages/liferay-client-extension.tgz"
LIFERAY_DEFAULT_HELM_URL="https://liferay-customer-solutions.github.io/liferay-portal/pages/liferay-default.tgz"
VERBOSE=""

for arg in "$@"; do
case $arg in
	--verbose)
	  VERBOSE="true"
	  shift
	  ;;
esac
done

function create_cluster {
	if ! k3d cluster get "${CLUSTER_NAME}" > /dev/null 2>&1; then
		echo "k3d cluster '${CLUSTER_NAME}' not found. Creating it now..."
		k3d cluster create ${CLUSTER_NAME} \
			--port "80:80@loadbalancer" \
			--registry-create registry:5000 \
			--volume "${PWD}/liferay:/mnt/local@all:*" \
			--wait
		echo "Cluster '${CLUSTER_NAME}' created."
	else
		echo "k3d cluster '${CLUSTER_NAME}' already exists."
	fi
}

function deploy_cx {
	local release_name=$1
	local build_needed=$2
	local yaml_file="${PWD}/helm/cx/${release_name}-values.yaml"

	if [ ! -f "${yaml_file}" ]; then
		return
	fi

	local enabled
	enabled=$(awk -v service="${release_name}" \
		'/^cx:/ { in_cx = 1 }
		in_cx && $0 ~ "  " service ":" { in_service = 1 }
		in_cx && in_service && /enabled:/ { print $2; exit }
	' "${PWD}/helm/default-values.yaml")

	if [[ "${enabled}" == "false" ]]; then
		if helm status "${release_name}" > /dev/null 2>&1; then
			section "Uninstalling disabled client extension: ${release_name}"
			helm uninstall "${release_name}" &
		fi
		return
	fi

	if [ -d "../client-extensions/${release_name}" ]; then
		if ! docker manifest inspect "localhost:5000/${release_name}" > /dev/null 2>&1; then
			build_needed=true
		fi
	fi

	section "Client Extension: ${release_name}"

	if [ "$build_needed" = true ] && [ -d "../client-extensions/${release_name}" ]; then
		echo "Building client extension: ${release_name}"
		if [[ -n "$VERBOSE" ]]; then
			(cd ../client-extensions && ../gradlew ":client-extensions:${release_name}:pushDockerImage")
		else
			(cd ../client-extensions && ../gradlew ":client-extensions:${release_name}:pushDockerImage" > /dev/null 2>&1)
		fi
		if [ $? -eq 0 ]; then
			echo "Client extension ${release_name} built successfully."
		else
			echo "Error: Client extension ${release_name} build failed."
		fi
	fi

	echo "Deploying client extension: ${release_name}"

	local default_values_args=()
	if [[ -f "${PWD}/helm/default-values.yaml" ]]; then
		default_values_args+=("--values" "${PWD}/helm/default-values.yaml")
	fi

	local cx_url=""

	local main_domain_from_cx_yaml=$(awk '/^[[:space:]]*mainDomain:/ {print $2}' "${yaml_file}" | tr -d '"')
	if [[ -n "${main_domain_from_cx_yaml}" ]]; then
		cx_url="http://${main_domain_from_cx_yaml}"
	fi

	local values_args=()
	values_args+=("--values" "${yaml_file}")

	local config_yaml_file="${PWD}/helm/cx/${release_name}-config-values.yaml"

	if [ -f "${config_yaml_file}" ]; then
		values_args+=("--values" "${config_yaml_file}")
	fi

	local helm_debug_arg=""
	if [[ -n "$VERBOSE" ]]; then
		helm_debug_arg="--debug"
		echo "Helm command for ${release_name}: helm upgrade --install ${release_name} ${LIFERAY_CLIENT_EXTENSION_HELM_URL} --set image.repository=registry:5000/${release_name} ${default_values_args[@]} ${values_args[@]} ${helm_debug_arg}"
	fi

	helm upgrade --install "${release_name}" "${LIFERAY_CLIENT_EXTENSION_HELM_URL}" \
		--set "image.repository=registry:5000/${release_name}" \
		"${default_values_args[@]}" \
		"${values_args[@]}" ${helm_debug_arg} &

	if [[ -n "${cx_url}" ]]; then
		echo "URL: ${cx_url}"
	fi

	echo "UNINSTALL: helm uninstall ${release_name}"
}

function download_hotfix {
	mkdir -p ./liferay/patching
	chown -R $(whoami) ./liferay

	for file_url in \
		"https://releases-cdn.liferay.com/dxp/hotfix/2025.q3.7/liferay-dxp-2025.q3.7-hotfix-16.zip" \
		"https://releases-cdn.liferay.com/tools/patching-tool/patching-tool-4.0.9.zip"
	do
		local file_name="./liferay/patching/$(basename "${file_url}")"

		if [ ! -f "${file_name}" ]
		then
			echo "Downloading ${file_url} to ${file_name}."

			curl --location "${file_url}" --output "${file_name}"
		fi
	done
}

function extract_license {
	LIC="license.xml"

	if stat "${LIC}" &>/dev/null; then
	echo "File '${LIC}' exists."
	else
		docker container rm --force liferay-dxp-latest && \
			docker create --pull always --name liferay-dxp-latest liferay/dxp:latest && \
			docker export liferay-dxp-latest | tar --extract --verbose --strip-components=3 --wildcards --directory . opt/liferay/deploy/*.xml && \
			mv trial-dxp-license*.xml ${LIC}
	fi
}

function install_dependencies {
	echo "Checking for dependencies..."

	PKG_MANAGER=""
	if command -v apt-get &> /dev/null; then
		PKG_MANAGER="apt-get"
	elif command -v dnf &> /dev/null; then
		PKG_MANAGER="dnf"
	elif command -v yum &> /dev/null; then
		PKG_MANAGER="yum"
	fi

	if ! command -v curl &> /dev/null; then
		echo "curl not found. Installing..." >&2
		if [ -n "$PKG_MANAGER" ]; then
			sudo "${PKG_MANAGER}" update
			sudo "${PKG_MANAGER}" install --assumeyes curl
		else
			echo "Cannot determine package manager. Please install curl manually." >&2
			exit 1
		fi
	fi

	if ! command -v docker &> /dev/null; then
		echo "Docker not found. Installing..." >&2
		curl --fail --silent --show-error --location https://get.docker.com --output get-docker.sh
		sudo sh get-docker.sh
		sudo usermod --append --groups docker "${USER}"
		echo "Docker installed. IMPORTANT: You may need to log out and log back in for user group changes to take effect." >&2
	fi

	if ! command -v helm &> /dev/null; then
		echo "Helm not found. Installing..." >&2
		curl --fail --silent --show-error --location --output get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
		chmod 700 get_helm.sh
		sudo ./get_helm.sh
	fi

	if ! command -v k3d &> /dev/null; then
		echo "k3d not found. Installing..." >&2
		curl --silent https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | sudo bash
	fi

	echo "All dependencies are present."
}

function patch_coredns {
	gateway_ip=$(docker inspect --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "k3d-${CLUSTER_NAME}-serverlb")

	cat "${PWD}/manifests/coredns-custom.yaml" | sed "s/__GATEWAY_IP__/${gateway_ip}/" | sed "s/__DOMAIN_SUFFIX__/${DOMAIN_SUFFIX}/" | kubectl apply -f -
}

function run_watch {
	section "Watching for changes..."

	local checksum_dir
	checksum_dir=$(mktemp -d)

	function get_checksum {
		local path=$1
		local checksum_file=$2
		shift 2
		local extra_args=($@)
		local files
		files=$(cd .. && git ls-files --cached --others --exclude-standard "${extra_args[@]}" "$path")
		if [ -n "$files" ]; then
			(
				cd ..
				echo "$files" | xargs -d '\n' md5sum
			) | sort -k 2 > "$checksum_file"
		else
			> "$checksum_file"
		fi
	}

	for dir in ../client-extensions/*/;
	do
		ce_name=$(basename "$dir")
		get_checksum "client-extensions/${ce_name}" "${checksum_dir}/${ce_name}.build.md5" --exclude='*/client-extension.yaml'
		get_checksum "client-extensions/${ce_name}/client-extension.yaml" "${checksum_dir}/${ce_name}.deploy.md5"
	done
	get_checksum "quickstart/helm" "${checksum_dir}/helm.md5"

	while true; do
		for dir in ../client-extensions/*/;
		do
			local ce_name
			ce_name=$(basename "$dir")

			get_checksum "client-extensions/${ce_name}" "${checksum_dir}/${ce_name}.build.md5.new" --exclude='*/client-extension.yaml'
			get_checksum "client-extensions/${ce_name}/client-extension.yaml" "${checksum_dir}/${ce_name}.deploy.md5.new"

			build_needed=false
			deploy_needed=false

			if ! cmp -s "${checksum_dir}/${ce_name}.build.md5" "${checksum_dir}/${ce_name}.build.md5.new"; then
				build_needed=true
			fi

			if ! cmp -s "${checksum_dir}/${ce_name}.deploy.md5" "${checksum_dir}/${ce_name}.deploy.md5.new"; then
				deploy_needed=true
			fi

			if $build_needed || $deploy_needed; then
				deploy_cx "${ce_name}" $build_needed
				mv "${checksum_dir}/${ce_name}.build.md5.new" "${checksum_dir}/${ce_name}.build.md5"
				mv "${checksum_dir}/${ce_name}.deploy.md5.new" "${checksum_dir}/${ce_name}.deploy.md5"
			else
				rm -f "${checksum_dir}/${ce_name}.build.md5.new" "${checksum_dir}/${ce_name}.deploy.md5.new"
			fi
		done

		get_checksum "quickstart/helm" "${checksum_dir}/helm.md5.new"
		if ! cmp -s "${checksum_dir}/helm.md5" "${checksum_dir}/helm.md5.new"; then
			echo "Changes detected in helm directory. Redeploying portals..."
			start_portal "dxp" "${LIFERAY_DEFAULT_HELM_URL}"
			mv "${checksum_dir}/helm.md5.new" "${checksum_dir}/helm.md5"
		else
			rm -f "${checksum_dir}/helm.md5.new"
		fi

		sleep 5
	done
}

function section {
	echo "============"
	echo "$1"
}

function start_client_extensions {
	if [[ -d "${PWD}/helm/cx" ]]; then
		mapfile -d '' -t yaml_files < <(find "${PWD}/helm/cx" -type f \( -name "*.yaml" -o -name "*.yml" \) -print0 | sort -z)

		for yaml_file in "${yaml_files[@]}"; do
			local file_name
			file_name=$(basename "${yaml_file}")
			local release_name="${file_name%-values.yaml}"
			deploy_cx "${release_name}" false
		done
	fi
}

function start_portal {
	local release_name=$1
	local namespace=$2 # Removed base_path, adjusted parameter index

	local namespace_args=()
	if [[ -n "${namespace}" ]]; then
		namespace_args+=("--namespace" "${namespace}")
	fi

	local enabled
	enabled=$(awk -v release="${release_name}" '
		/^[a-zA-Z]/ { in_release = 0; in_liferay = 0 }
		$0 ~ "^" release ":" { in_release = 1 }
		in_release && /^[[:space:]]+liferay:/ { in_liferay = 1 }
		in_release && in_liferay && /^[[:space:]]+enabled:/ { print $2; exit }
	' "${PWD}/helm/default-values.yaml")

	if [[ "${enabled}" == "false" ]]; then
		if helm status "${release_name}" "${namespace_args[@]}" > /dev/null 2>&1; then
			section "Uninstalling disabled portal: ${release_name}"
			helm uninstall "${release_name}" "${namespace_args[@]}" &
		fi
		return
	fi

	section "Portal: ${release_name}"

	local helm_debug_arg=""
	if [[ -n "$VERBOSE" ]]; then
		helm_debug_arg="--debug"
	fi

	values_args=()

	local portal_url=""
	local ingress_values_file="${PWD}/helm/${release_name}/ingress-values.yaml"

	if [[ -f "${ingress_values_file}" ]]; then
	 	portal_url=$(awk '/^[[:space:]]*-?[[:space:]]*host:/{print $3}' "${ingress_values_file}" | tr -d '"')
	fi

	if [[ -d "${PWD}/helm/${release_name}" ]]; then
		mapfile -d '' -t yaml_files < <(find "${PWD}/helm/${release_name}" -type f \( -name "*.yaml" -o -name "*.yml" \) -print0 | sort -z)

		for yaml_file in "${yaml_files[@]}"; do
			values_args+=("--values" "${yaml_file}")
		done
	fi

	local create_namespace_args=()
	if [[ -n "${namespace}" ]]; then
		create_namespace_args+=("--create-namespace")
	fi

	kubectl create secret generic liferay-default "${namespace_args[@]}" \
	  --from-literal=LIFERAY_DEFAULT_PERIOD_ADMIN_PERIOD_PASSWORD="test" \
	  --dry-run=client -o yaml | kubectl apply -f -

	helm upgrade --install ${release_name} "${LIFERAY_DEFAULT_HELM_URL}" \
		"${create_namespace_args[@]}" \
		"${namespace_args[@]}" \
		--set-file "configmap.data.license\.xml=license.xml" \
		"${values_args[@]}" ${helm_debug_arg}

	if [[ -n "${portal_url}" ]]; then
		echo "URL: http://${portal_url}"
	fi

	echo "UNINSTALL: helm uninstall ${release_name} ${namespace_args[@]}"
}

function update_registry {
	for service in pubsub jsmsync maildev; do
		local enabled
		enabled=$(awk -v service="${service}" '
			/^cx:/ { in_cx = 1 }
			in_cx && $0 ~ "  " service ":" { in_service = 1 }
			in_cx && in_service && /enabled:/ { print $2; exit }
		' "${PWD}/helm/default-values.yaml")

		if [[ "${enabled}" == "false" ]]; then
			continue
		fi

		if curl --silent "http://localhost:5000/v2/${service}/tags/list" | grep -q "latest"; then
			continue
		fi

		local image_to_pull=""
		case $service in
			pubsub)
				image_to_pull="google/cloud-sdk"
				;;
			jsmsync)
				image_to_pull="liferay/meta-inf"
				;;
			maildev)
				image_to_pull="maildev/maildev"
				;;
		esac

		if [[ -n "${image_to_pull}" ]]; then
			docker pull "${image_to_pull}"
			docker tag "${image_to_pull}:latest" "localhost:5000/${service}"
			docker push "localhost:5000/${service}"
		fi
	done
}

function main {
	install_dependencies

	download_hotfix

	extract_license

	create_cluster

	patch_coredns

	sleep 5

	start_portal "dxp"

	update_registry

	start_client_extensions

	run_watch
}

main "${@}"
