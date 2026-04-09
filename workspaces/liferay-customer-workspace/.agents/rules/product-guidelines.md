# Product Guidelines: Liferay Customer Workspace

## 1. Design & Branding (Liferay Ecosystem)
- **Token-Driven Styling:** All custom CSS and component styling must utilize Liferay Classic Theme CSS tokens (e.g., `var(--primary)`, `var(--secondary)`) whenever possible to ensure seamless integration and dynamic theme updates.
- **Consistent UI/UX:** Custom React elements should visually align with standard Liferay Clay components. Maintain consistent padding, typography, and interactive states.
- **Responsive by Default:** All frontend extensions and fragments must be fully responsive, prioritizing mobile-first fluid layouts.

## 2. Performance Optimization
- **Asset Management:** Avoid heavy third-party UI libraries if a lightweight native solution exists. Defer non-critical JavaScript execution.

## 3. Code & Architecture (Client Extensions)
- **Modularity:** Maintain strict separation of concerns across different client extension types (Custom Elements, Global CSS, Spring Boot backends, Site Initializers).
- **Headless First:** When integrating data, prefer Liferay's Headless REST APIs. Ensure robust error handling and fallback states if endpoints are unreachable.
- **TypeScript Strictness:** Enforce strict type checking in all React custom elements to catch integration errors early and document data contracts.

## 4. Liferay Architectural Conventions
- **Version Alignment:** For Liferay DXP 7.4+ or quarterly releases, always favor Client Extensions and Fragments over traditional OSGi modules.
- **Data Persistence:** Use Liferay Objects for data storage unless a dedicated external database is explicitly required.
- **Form Patterns:** Use `Liferay.Util.fetch` for all form submissions to ensure CSRF token inclusion.
- **Resource Management:** Reference static assets using the `[resources:filename.ext]` syntax in fragments.

## 5. Project Planning & Management

- **Jira Integration:** Use the Jira MCP to reference and update tasks. All implementation plans should be informed by the requirements and context provided in Jira tickets.


## 4. Content & Tone
- **Professional & Clear:** Use concise, instructional language for administrative interfaces.
- **Action-Oriented:** Button labels and call-to-actions should be verb-led (e.g., "Initialize Site", "Sync Data").
- **Helpful Error States:** Error messages must be descriptive, user-friendly, and offer actionable resolution steps rather than exposing raw stack traces.