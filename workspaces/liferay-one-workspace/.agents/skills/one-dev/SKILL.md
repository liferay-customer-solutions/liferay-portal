---

allowed-tools: [Bash, Glob, Grep, Read]
description: Start a live Vite dev server for a `liferay-one-*` custom element and point the running Liferay at it for hot-module reload. Use when the user asks to run dev mode, start Vite, get HMR, or invokes /one-dev.
name: one-dev

---

# Live Vite Dev for One Workspace Custom Elements

Run a custom element from the Vite dev server instead of its built static assets, so source edits hot-reload in the browser without a full redeploy. This mirrors `gradlew deployDev` from `liferay-customer-workspace`.

The workspace Gradle plugin scans each `client-extension.<profile>.yaml` (regex `^client-extension\.([a-z]+)\.yaml$`) and generates a `deploy<Profile>` task. So `client-extension.dev.yaml` produces a `deployDev` task that deploys the extension with its `urls` pointed at the Vite dev server (`http://localhost:5173/...`) rather than the bundled `*.js`. The browser then loads modules — and the HMR client — straight from Vite.

Run everything from `workspaces/liferay-one-workspace/`.

## 1. Resolve Target

Valid targets are `liferay-one-*` client extensions that have a `client-extension.dev.yaml`. Today that is `liferay-one-custom-element`. Confirm the file exists:

```bash
ls client-extensions/liferay-one-custom-element/client-extension.dev.yaml
```

Note the port the dev yaml expects (the `urls` host, e.g. `http://localhost:5173`). Vite must bind that exact port — it is baked into the deployed extension.

## 2. Pre-flight

Liferay must be running. Confirm the container and HTTP port:

```bash
docker ps --filter "name=^liferay$" --quiet
curl --fail --silent --output /dev/null http://localhost:8080/c/portal/status && echo ready
```

If either fails, start the environment first (`/one-env-up`).

Ensure dependencies are installed (the workspace is a single yarn workspace with hoisted `node_modules`):

```bash
[ -x client-extensions/liferay-one-custom-element/node_modules/.bin/vite ] || yarn install
```

## 3. Start the Vite Dev Server

Start Vite in the background, pinned to the port the dev yaml expects. `--strictPort` makes Vite fail loudly instead of silently bumping to 5174 (which would leave the deployed extension pointing at a dead port):

```bash
(cd client-extensions/liferay-one-custom-element && yarn dev --port 5173 --strictPort)
```

Run this with `run_in_background`. Then wait until Vite answers:

```bash
curl --fail --silent --output /dev/null http://localhost:5173/@vite/client && echo "vite up"
```

Vite serves cross-origin to the Liferay page (CORS is on by default in Vite 4), and `vite.config.ts` already sets `server.origin` so the HMR websocket connects correctly.

## 4. Deploy the Dev Profile

Deploy the extension with the dev yaml overlay into the running container:

```bash
./gradlew :client-extensions:liferay-one-custom-element:deployDev \
    -Ddeploy.docker.container.id=$(docker ps --filter "name=^liferay$" --quiet)
```

## 5. Verify

Confirm Liferay re-registered the extension, watching the container log for the LPKG/extension pickup:

```bash
docker logs --tail 50 $(docker ps --filter "name=^liferay$" --quiet) 2>&1 | grep -i "liferay-one-custom-element\|client extension"
```

Then load a page hosting the custom element in the browser and confirm the module requests resolve to `http://localhost:5173/src/main.tsx` (DevTools → Network). Editing a file under `client-extensions/liferay-one-custom-element/src` now hot-reloads in place.

## 6. Return to Production Assets

Dev mode is sticky: the extension keeps pointing at `localhost:5173` until redeployed normally. When finished, stop the background Vite process and restore the bundled static assets with a normal deploy (`/one-deploy`):

```bash
./gradlew :client-extensions:liferay-one-custom-element:clean :client-extensions:liferay-one-custom-element:deploy \
    -Ddeploy.docker.container.id=$(docker ps --filter "name=^liferay$" --quiet)
```

Report: which extension is in dev mode, the Vite URL, deploy result, and log evidence of pickup.