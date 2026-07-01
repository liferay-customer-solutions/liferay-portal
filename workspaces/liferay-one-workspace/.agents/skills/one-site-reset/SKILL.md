---

allowed-tools: [Bash, Glob, Grep, Read]
description: Reset just the "One" site in the running local Liferay — tear down seeded records, delete the site, redeploy the site initializer, and reseed. Use when only the site content or seeded data is stale.
name: one-site-reset

---

# Reset the One Site

Run from `workspaces/liferay-one-workspace/`.

The lightest of the three resets. It never touches Docker, the database, or the
other client extensions, so it is far faster than `/one-instance-reset` or
`/one-env-reset`, and it leaves the local-dev OAuth2 application intact. Use it
when only the site's pages and content, or the seeded records, have drifted —
not when object definitions, roles, or taxonomies (owned by the batch client
extension) need to change.

## Prerequisite

The Liferay containers must already be running (`docker ps` shows a healthy
`liferay` container). If they are not, start them with `/one-env-up` first.

## Run

```bash
scripts/site_reset.sh
```

The script pins basic auth against localhost, then:

1. Tears down the seeded records (`scripts/seed/teardown_records.sh`).

1. Deletes the `One` site group through the JSONWS bridge.

1. Redeploys only the `liferay-one-site-initializer` client extension, which
   recreates and re-initializes the site, and waits for the
   `Initialized One for group` log marker.

1. Reseeds the data (`scripts/seed.sh`).

1. Rebinds the `one.localhost` virtual host
   (`scripts/bootstrap/set_virtual_hosts.sh`).

It finishes with `Done. The One site has been reset.`

## When To Reach For A Heavier Reset

- Object definitions, relationships, roles, or taxonomies changed → these are
  owned by the batch client extension, so use `/one-instance-reset`.
- The database is corrupt, the image or bundle changed, or the instance itself
  is broken → use `/one-env-reset`.