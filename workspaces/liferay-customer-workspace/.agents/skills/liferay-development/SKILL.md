---
name: liferay-development
description: Expert guidance for building, styling, and configuring Liferay fragments and client extensions (both frontend and server-side). Use when creating or modifying UI components, mapping CSS tokens, defining fragment configuration schemas, or building microservices for Object Actions, Workflow Actions, and Proxy Objects.
---

# Liferay Development Skill

This skill provides expert procedural knowledge for developing high-quality Liferay fragments and client extensions, including both frontend components and server-side microservices.

## Core Workflows

- **Building Fragments**: Follow best practices for HTML structure, CSS scoping, and JavaScript initialization.
- **Configuration & Editability**: Define robust `configuration.json` schemas and make content editable using `data-lfr-editable` attributes.
- **Styling with Tokens**: Map fragment CSS variables to Liferay Classic theme CSS tokens for site-wide brand consistency.
- **Frontend Client Extensions**: Guidance for creating and deploying Global CSS/JS and Custom Element extensions.
- **Server-Side Client Extensions**: Scaffolding, configuring, and deploying Object Actions, Workflow Actions, and Proxy Objects as microservices.
- **Liferay Cloud PaaS Deployment**: Structuring the workspace to deploy both client extension definitions and backend microservices using `LCP.json`.

## Architecture Preference: Node.js (Server-Side)

For building demo microservices, **Node.js (with Express)** is the preferred and default framework over Spring Boot. 
- **Speed & Simplicity**: Scaffolding endpoints requires minimal boilerplate, keeping demo repositories clean and easy to explain.
- **Native JSON**: Liferay Client Extensions communicate via JSON payloads, which Node.js processes natively without complex object mapping.
- **Resource Efficiency**: Node.js containers have a significantly smaller memory footprint and faster startup times in Liferay Cloud PaaS compared to Java/Spring Boot.

*Note: Only use Spring Boot if the demo specifically requires an enterprise Java narrative or integration with heavy Java-based legacy systems.*

## STRICT EXECUTION PROTOCOL (MANDATORY READS)

You MUST NOT rely on pre-existing Liferay knowledge. Your pre-trained knowledge is outdated or incorrect for this specific environment. You MUST use the `read_file` tool to read the following reference documents BEFORE beginning execution or strategy planning:

- **General Liferay Tasks**: You MUST read **[LIFERAY_BEST_PRACTICES.md](references/LIFERAY_BEST_PRACTICES.md)** before writing any code.
- **Fragment Development**: You MUST read **[LIFERAY_FRAGMENT_DEVELOPMENT_GUIDE.md](references/LIFERAY_FRAGMENT_DEVELOPMENT_GUIDE.md)**.
- **Fragment Configuration (`configuration.json`)**: You MUST read **[FRAGMENT_LFR_CONFIGURATION_TYPES.md](references/FRAGMENT_LFR_CONFIGURATION_TYPES.md)** to ensure correct JSON syntax and avoid using deprecated field types.
- **Fragment Editability (HTML `data-lfr-editable-type`)**: You MUST read **[FRAGMENT_LFR_EDITABLE_TYPES.md](references/FRAGMENT_LFR_EDITABLE_TYPES.md)** before applying editable tags to HTML elements (e.g., `<a>`, `<img>`, `<h1>`). Do not guess these types.
- **React Client Extensions**: For React-based Custom Elements, you MUST read **[REACT_CUSTOM_ELEMENT_CLIENT_EXTENSION_GUIDE.md](references/REACT_CUSTOM_ELEMENT_CLIENT_EXTENSION_GUIDE.md)**.
- **Batch Client Extensions**: For Object/Folder initialization, you MUST read **[BATCH_OBJECT_CLIENT_EXTENSION_GUIDE.md](references/BATCH_OBJECT_CLIENT_EXTENSION_GUIDE.md)**.
- **Server-Side Configuration & Microservices**: You MUST read **[LIFERAY_SERVER_CX_GUIDE.md](references/LIFERAY_SERVER_CX_GUIDE.md)** when working with Object Actions, Workflow Actions, or Proxy Objects.
- **Styling**: You MUST read **[LIFERAY_CORE_STYLEBOOK_CLASSIC_CSS_TOKENS.md](references/LIFERAY_CORE_STYLEBOOK_CLASSIC_CSS_TOKENS.md)** before applying CSS colors or variables.
- **Headless APIs**: You MUST read **[LIFERAY_HEADLESS_API_GUIDE.md](references/LIFERAY_HEADLESS_API_GUIDE.md)** before writing any API interaction logic.
