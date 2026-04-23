---

allowed-tools: [Bash, Glob, Grep, Read]
description: Manage the local Liferay Docker environment for the liferay-one-workspace. Use when the user asks to bootstrap, start, stop, restart, or reset the local Liferay container.
name: one-env

---

# Manage Liferay One Environment

Run from `workspaces/liferay-one-workspace/`.

## First Run (Bootstrap)

Run `scripts/bootstrap.sh` with no arguments. It builds the Docker image, tags it, starts the containers, waits for Liferay to be healthy, and deploys the client extensions.

```bash
scripts/bootstrap.sh
```

Liferay is ready when it prints `Done. Liferay is running at http://localhost:8080.`

## Day-to-Day

After the first run, use `docker compose` directly:

```bash
# Start containers
docker compose up --detach

# Stop containers (keeps volumes)
docker compose stop

# Tear down (keeps volumes)
docker compose down

# Full reset — wipes all volumes
docker compose down --volumes
```