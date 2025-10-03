{{/*
DXP ConfigMap name
*/}}
{{- define "dxp-configmap.name" -}}
{{- printf "%s-lxc-dxp-metadata" .Values.clientExtensionConfig.virtualInstanceId }}
{{- end }}

{{/*
App name
*/}}
{{- define "liferay-client-extension.appname" -}}
{{- default .Release.Name .Values.nameOverride .Chart.Name | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "liferay-client-extension.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Init Provision ConfigMap name
*/}}
{{- define "liferay-client-extension.ext-init-configmap.name" -}}
{{- printf "%s-%s-lxc-ext-init-metadata" .Release.Name .Values.clientExtensionConfig.virtualInstanceId }}
{{- end }}

{{/*
Ext Provision ConfigMap annotations
*/}}
{{- define "liferay-client-extension.ext-provision-configmap.annotations" -}}
ext.lxc.liferay.com/domains: {{ .Values.clientExtensionConfig.mainDomain }}
ext.lxc.liferay.com/mainDomain: {{ .Values.clientExtensionConfig.mainDomain }}
{{- with .Values.annotations }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Ext Provision ConfigMap labels
*/}}
{{- define "liferay-client-extension.ext-provision-configmap.labels" -}}
lxc.liferay.com/metadataType: "ext-provision"
{{ include "liferay-client-extension.labels" . }}
{{- end }}

{{/*
Ext Provision ConfigMap name
*/}}
{{- define "liferay-client-extension.ext-provision-configmap.name" -}}
{{- printf "%s-%s-lxc-ext-provision-metadata" .Release.Name .Values.clientExtensionConfig.virtualInstanceId }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "liferay-client-extension.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "liferay-client-extension.labels" -}}
helm.sh/chart: {{ include "liferay-client-extension.chart" . }}
{{ include "liferay-client-extension.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- if .Values.clientExtensionConfig.virtualInstanceId }}
dxp.lxc.liferay.com/virtualInstanceId: {{ .Values.clientExtensionConfig.virtualInstanceId }}
{{- end }}
ext.lxc.liferay.com/serviceId: {{ include "liferay-client-extension.appname" . }}
{{- with .Values.labels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "liferay-client-extension.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "liferay-client-extension.selectorLabels" -}}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/name: {{ include "liferay-client-extension.name" . }}
app: {{ include "liferay-client-extension.appname" . }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "liferay-client-extension.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "liferay-client-extension.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
