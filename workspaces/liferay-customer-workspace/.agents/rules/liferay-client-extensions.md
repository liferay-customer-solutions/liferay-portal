# Liferay Client Extension Protocol

This document provides guidance for generating, deploying, and verifying Liferay Client Extensions.

## 1. Unified Implementation Patterns
When building data-driven features (e.g., a form that stores data in Liferay), implement both the backend and frontend components concurrently:

1.  **Object Definition (Batch):** Define the schema (fields, types) in a batch client extension using `*.batch-engine-data.json`.
2.  **UI Component (Custom Element):** Create a Custom Element client extension (typically React) to render the form and interact with the Object's REST API.

## 2. Security & Data Integrity
- **CSRF Protection:** Always use **`Liferay.Util.fetch`** when making requests to Liferay APIs from Custom Elements. This ensures Liferay attaches session cookies and the critical CSRF token (`p_auth`), avoiding `403 Forbidden` errors.
- **Batch Field Requirements:** 
    - `indexedLanguageId` is only for `String` and `Clob` types.
    - `timeStorage` is **REQUIRED** for `Date` or `DateTime` fields (use `convertToUTC` or `useInputAsFormatted`).

## 3. Deployment & Lifecycle
- **Blade Deployment:** Use `blade gw deploy` to package the extension into a `.zip` and copy it to the server's `osgi/client-extensions` directory.
- **Verification:** 
    - Check server logs for the `STARTED [extension-id]` entry.
    - In Liferay UI, look for the component in the **Widgets** tab under the **Client Extensions** category during page editing.

## 4. Source of Truth
- Always check the `client-extension.yaml` file for valid property configurations.
- No `build.gradle` is needed inside extension folders; the Liferay Workspace plugin handles them automatically.
