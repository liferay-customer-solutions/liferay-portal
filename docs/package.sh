#!/bin/bash
helm package ../cloud/helm/default
helm package ../cloud/helm/client-extension
cp liferay-default-*.tgz liferay-default.tgz
cp liferay-client-extension-*.tgz liferay-client-extension.tgz
helm repo index .
