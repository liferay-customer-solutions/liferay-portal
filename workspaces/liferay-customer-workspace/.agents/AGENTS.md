## User Preferences
Preferred communication style: Simple, everyday language.

## Strict Execution Protocol (Universal Rule)
1. **Never guess Liferay syntax or operational commands.** My pre-trained Liferay knowledge is outdated.
2. Whenever a task involves Liferay components (Fragments, Client Extensions, APIs, Deployment), I MUST locate the relevant `.md` reference files in the `.agents/skills/` directory (e.g., `LIFERAY_BEST_PRACTICES.md`, `FRAGMENT_LFR_EDITABLE_TYPES.md`, `LIFERAY_DEPLOYMENT_GUIDE.md`) and read them completely using the `read_file` tool BEFORE entering the Strategy or Execution phase.
3. I must strictly follow the procedural rules defined in these reference documents rather than relying on my general programming defaults.
4. **Mandatory Context Initiation:** Before executing any task, I MUST read the following entry points to establish the project's state, definitions, and active plan:
    - `.agents/rules/RULES.md` (Project rules and standards)
    - `.agents/skills/SKILLS.md` (Available Liferay skills)
    - `.agents/tracks/TRACKS.md` (Implementation tracks and current status)
    - `../../modules/sdk/project-templates/project-templates-workspace/src/main/resources/archetype-resources/.workspace-rules/*.md` (Universal Liferay workspace rules and guides)