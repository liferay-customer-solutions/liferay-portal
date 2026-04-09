# Skill: Safe Research & Discovery Tooling

This skill authorizes and guides the AI agent in the autonomous use of "safe" read-only tools for project analysis, technical research, and troubleshooting.

## 1. Authorized Tooling (Pre-Approved)

The following tools are considered "safe" for autonomous execution during the **Inquiry** (Research/Analysis) and **Planning** phases:

- **File Discovery:** `find`, `ls`, `glob`
- **Content Analysis:** `cat`, `grep`, `read_file`, `grep_search`
- **Source Control History:** `git log`, `git diff`, `git show`, `git status`
- **Build Inspection:** `ls -R`, `du -h`

## 2. Operational Rules

1. **Autonomous Research:** You do NOT need to ask for permission to use the tools listed above when they are used for discovery, understanding a codebase, or investigating a bug.
2. **Context Efficiency:** Use `grep` and `find` strategically to locate information without reading entire directories or large files unnecessarily.
3. **No Mutations:** This skill does NOT authorize autonomous file modifications (`write_file`, `replace`), deletions (`rm`), or system configuration changes (`chmod`, `chown`) outside of the approved `conductor/` planning process.
4. **Information Synthesis:** Use the output of these tools to inform your technical opinions and implementation plans.

## 3. Usage Patterns

- **Locating a symbol:** `grep -r "SymbolName" src/`
- **Finding a file by name:** `find src/ -name "*.tsx"`
- **Reviewing recent changes:** `git log -n 5`
- **Checking differences:** `git diff HEAD`
