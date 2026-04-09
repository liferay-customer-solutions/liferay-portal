# Liferay Workspace Setup & Operations

This document provides instructions for initializing and operating the Liferay environment.

## 1. Workspace Verification
Verify the root directory contains:
- `gradle.properties`
- `settings.gradle`
If missing, initialize with `blade init -v [version]`.

## 2. Server Initialization
- **Download/Setup:** Run `blade server init` to download the Liferay Portal (Tomcat bundle) into the `/bundles` folder.
- **Verification:** Ensure the `/bundles` folder exists before attempting to start the server.

## 3. Server Operations
- **Start:** `blade server start` (use `-t` to tail logs, `-d` for debug mode).
- **Run:** `blade server run` (starts in the foreground).
- **Credentials:** Default login is `test@liferay.com` with password `test`.
- **Logs:** Monitor `bundles/tomcat/logs/catalina.out` for "Server startup in [X] ms".

## 4. Troubleshooting
- Query [liferay-learn](https://learn.liferay.com) for error messages.
- Common issues are documented in `/w/dxp/self-hosted-installation-and-upgrades`.
