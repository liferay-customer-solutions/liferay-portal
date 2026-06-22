---

allowed-tools: [Bash, Read]
description: Pull the workspace secrets from the 1Password secure note ".env [Liferay One Workspace]" and write them to the local .env file, backing up the previous copy first.
name: one-env-pull

---

# Pull Liferay One Workspace .env from 1Password

Run from `workspaces/liferay-one-workspace/`.

The canonical workspace secrets live in a 1Password **secure note** named `.env [Liferay One Workspace]`. Its body is a verbatim `.env` file (the `LIFERAY_ONE_*` keys consumed by the `liferay-one-etc-spring-boot` client extension via `docker-compose.yaml`). This skill copies that note into the local `.env`, replacing whatever is there.

The local `.env` is gitignored, so it never reaches version control.

## 1. Confirm 1Password Access

The 1Password CLI must be installed and signed in to the `liferayinc.1password.com` account.

```bash
op account list
```

If no account is listed or a later step reports an authentication error, sign in and retry:

```bash
eval "$(op signin)"
```

## 2. Fetch the Note and Update `.env`

Fetch the note body into a variable first, verify it is non-empty, then back up the existing `.env` to `.env.bak` before overwriting. Never clobber `.env` with an empty or failed fetch.

```bash
note="$(
	op item get ".env [Liferay One Workspace]" --format json |
		jq -r '.fields[] | select(.purpose == "NOTES" or .id == "notesPlain") | .value'
)"

if [ -z "${note}" ]; then
	echo "Unable to read the secure note '.env [Liferay One Workspace]'. Confirm the note exists and op is signed in."
	exit 1
fi

[ -f .env ] && cp .env .env.bak

printf '%s\n' "${note}" > .env

echo "Wrote $(grep -c '=' .env) entries to .env (previous copy saved to .env.bak)."
```

If `op item get` reports more than one match, the note title is ambiguous across vaults — scope it with `--vault "<Vault Name>"`.

## 3. Apply the Changes

The Spring Boot container reads `.env` only at container creation. Recreate it so the new values take effect:

```bash
docker compose up -d liferay-one-etc-spring-boot
```

## Notes

- This skill is **pull-only**. To publish local changes back, update the secure note in the 1Password app — keep the note body in exact `.env` syntax (`KEY=VALUE`, one per line, JSON values such as the GCS service account key on a single unquoted line).
- Keys absent from the note fall back to the `=unused` defaults that `buildDockerImage` writes to `build/local.env`, which `docker-compose.yaml` loads ahead of `.env`.