#!/bin/bash

NODEHOST_TO_APPEND="$1"

K3D_INTERNAL_HOST="host.k3d.internal"

coredns_configmap_file=$(mktemp /tmp/coredns-configmap.XXXXXX)
new_coredns_configmap_file="${coredns_configmap_file}.new"

kubectl get configmap coredns -n kube-system -o yaml >"$coredns_configmap_file"

sanitize () {
	tr '\n' '\f'
}

desanitize () {
	tr '\f' '\n'
}

K3D_INTERNAL_HOST_LINE=$(sed -n "/${K3D_INTERNAL_HOST}\$/p" "$coredns_configmap_file")
NEW_NODEHOST_LINE="${K3D_INTERNAL_HOST_LINE/$K3D_INTERNAL_HOST/$NODEHOST_TO_APPEND}"
NEW_NODEHOSTS_SECTION=$(printf '%s\n%s' "$K3D_INTERNAL_HOST_LINE" "$NEW_NODEHOST_LINE")
NEW_NODEHOSTS_SECTION_SANITIZED=$(printf "$NEW_NODEHOSTS_SECTION" | sanitize)

cat "$coredns_configmap_file" | sed -e "s/${K3D_INTERNAL_HOST_LINE}/${NEW_NODEHOSTS_SECTION_SANITIZED}/" | desanitize >"$new_coredns_configmap_file"

kubectl apply -f "$new_coredns_configmap_file"

kubectl rollout restart deployments/coredns -n kube-system
