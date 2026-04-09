# Tech Stack: Liferay Customer Workspace

## 1. Languages
- **Java:** Primary backend language for Liferay extensibility and Spring Boot microservices.
- **TypeScript:** Enforces type safety and modern JavaScript practices in frontend development.
- **CSS:** Utilized alongside Liferay Classic Theme tokens for styling components.
- **HTML:** Semantic markup for Liferay fragments and basic structures.

## 2. Platform & CMS
- **Liferay DXP:** The core digital experience platform providing identity management, content delivery, and portal capabilities.
- **Liferay Cloud (LXC):** The managed PaaS environment targeting deployment, offering continuous delivery and robust infrastructure.

## 3. Frontend Frameworks & Tooling
- **React:** The primary library for building interactive, component-driven user interfaces within Custom Element Client Extensions.
- **Vite:** Next-generation frontend tooling providing rapid compilation and a seamless development experience for React extensions.

## 4. Backend Frameworks & Tooling
- **Spring Boot:** The framework of choice for developing scalable microservices and backend API integrations deployed as Liferay Client Extensions.

## 5. Package Management & Build Tools
- **Gradle (Liferay Workspace):** Orchestrates the overall build process, manages Liferay dependencies, and packages modules for deployment.
- **Yarn:** The primary package manager for managing Node.js dependencies within frontend client extension directories.
- **Bnd:** Used for defining OSGi module metadata (bnd.bnd) and managing bundle manifests.

## 6. Project Management & Planning
- **Jira:** Primary tool for issue tracking and requirement management. Integrated into the planning workflow via Jira MCP tools.
- **Blade CLI:** The authoritative command-line tool for Liferay development, used for project creation, server management, and deployment.