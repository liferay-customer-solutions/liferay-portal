# Protocol: Cross-Agent Interoperability (Claude, Gemini, etc.)

This document defines how different AI agents (e.g., Gemini, Claude) must collaborate using the Conductor directory as their shared source of truth.

## 1. Universal Source of Truth

Every agent, regardless of its platform or provider, MUST adhere to the following hierarchy of project context:

1.  **Project Root `AGENTS.md`:** Initial instructions that mandate the use of Conductor.
2.  **`.agents/rules/RULES.md`:** The entry point for all project definitions and workflows.
3.  **Active Track `plan.md`:** The exact state of implementation and the source of truth for current tasks.

## 2. Mandatory Protocol for Claude / New Agents

When Claude (or any other agent) joins the project, its very first step should be to read the project context. You can include this in Claude's project-specific system instructions or as a `.clauderules` file in the project root:

> "You are working on a project managed by the Conductor methodology. Your primary source of truth is the `/.workspace-rules` directory. Before executing any tasks, you MUST:
> 1. Read `.agents/rules/RULES.md` to understand the project definition, tech stack, and workflow.
> 2. Read the active track's `plan.md` (found in `.agents/tracks/`) to determine the current task status.
> 3. Follow the `.agents/rules/workflow.md` for task execution, testing, and committing.
> 4. Use the approved project-specific commands (e.g., formatting and Jira lookups) documented in the workflow.
> 5. Refer to specialized procedural knowledge in the `.agents/skills/` directory when performing Liferay-specific tasks."

## 3. Seamless Handoff Strategy

To avoid duplication and state loss when switching agents:

- **Record the SHA:** Always append the 7-character commit SHA to `plan.md` after every completed task. This allows the next agent to verify the exact code state.
- **Handoff Notes:** If you must stop work in the middle of a complex task, leave a brief, dated note in the `plan.md` under the current task (e.g., `> HANDOFF (2025-01-15): Completed UI layout, but still need to wire up the API event handler.`).
- **Standardized Commits:** Use the Conductor commit format (`feat(scope): ...`) so the Git log remains a readable history for all agents.
- **Shared Skills:** All project-specific "skills" (like the formatting command or Jira lookup workflows) should be documented in the `.agents/rules/` directory or `.agents/skills/` and referenced by all agents.
