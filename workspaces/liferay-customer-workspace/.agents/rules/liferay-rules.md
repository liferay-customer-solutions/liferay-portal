# Liferay General Rules

This document outlines architectural and operational rules for Liferay Workspaces, derived from standard Liferay project templates.

## 1. Context Priming
Before answering technical questions or proposing changes, you MUST establish the environment context:
- Scan for `gradle.properties` in the root directory.
- Identify the value of `liferay.workspace.product` and `liferay.workspace.target.platform.version`.

## 2. Liferay Version-Aware Logic
Apply the following logic based on the identified Liferay version:
- **If Version < 7.4:** Focus on traditional OSGi module development (Portlets, Service Builder).
- **If Version >= 7.4 or Quarterly Release (Q):** Prioritize modern Liferay best practices like **Client Extensions**, **Fragments**, and **Objects**.
    - Only suggest traditional OSGi modules if Client Extensions cannot fulfill the requirements.
    - Reference the `liferay-learn` documentation to understand the purposes of different Client Extension types.
- Ensure all suggested Gradle dependencies align with the `target.platform.version` defined in the project workspace.

## 3. Authoritative Information Sources
- **Primary Documentation:** [liferay-learn](https://learn.liferay.com)
    - Topic examples: `/w/dxp/development/client-extensions`, `/w/dxp/low-code/objects`, `/w/dxp/development/developing-page-fragments`.
- **Source Code Patterns:** [liferay-portal](https://github.com/liferay/liferay-portal)
    - Reference the [Client Extension Samples](https://github.com/liferay/liferay-portal/tree/master/workspaces/liferay-sample-workspace/client-extensions) for working templates and valid YAML configurations.

## 4. Key Project Paths
- **Logs:** `bundles/tomcat/logs/`
- **Configs/Properties:** `configs/common/` or `configs/[env]/` (e.g., `configs/local/`).
- **OSGi Configs:** `configs/[env]/osgi/configs/` (source) and `bundles/osgi/configs/` (runtime).
- **Modules:** `modules/`
- **Client Extensions:** `client-extensions/`

## 5. Tooling Guidelines
- **Blade CLI:** Prefer `blade` over direct `gradlew` usage.
    - `blade gw tasks`: View available Gradle tasks.
    - `blade gw deploy`: Deploy code to the running server.
- **MCP Server:** Available on Liferay 2025.Q4 and later. Use it as the default tool for querying content and managing objects when enabled via `feature.flag.LPD-63311=true`.
