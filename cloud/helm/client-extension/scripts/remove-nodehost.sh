#!/bin/bash

NODEHOST_TO_REMOVE="$1"

coredns_configmap_file=$(mktemp /tmp/coredns-configmap.XXXXXX)
new_coredns_configmap_file="${coredns_configmap_file}.new"

kubectl get configmap coredns -n kube-system -o yaml >"$coredns_configmap_file"

cat "$coredns_configmap_file" | sed -e "/${NODEHOST_TO_REMOVE}\$/d" >"$new_coredns_configmap_file"

kubectl apply -f "$new_coredns_configmap_file"

kubectl rollout restart deployments/coredns -n kube-system
