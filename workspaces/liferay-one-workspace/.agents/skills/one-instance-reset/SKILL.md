---

allowed-tools: [Bash, Glob, Grep, Read]
description: Reset the Liferay virtual instance's data in place — tear down records and structure, delete the One site, redeploy the batch and site-initializer client extensions, and reseed. No Docker, database, or server restart.
name: one-instance-reset

---

# Reset the Liferay One Instance Data

Run from `workspaces/liferay-one-workspace/`.

The middle reset tier. It rebuilds the virtual instance's data entirely through
the Liferay APIs and client-extension redeploys — it never touches Docker, the
database, or the server process. Reach for it when the batch-owned scaffolding
that `/one-site-reset` leaves in place — object definitions, relationships,
commerce configuration, roles, taxonomies — needs to be rebuilt, but the image
and bundle are still good. When the image, hotfix, license, or Dockerfile
changed, use `/one-env-reset` instead.

## Why Not "Create A New Instance, Delete The Old One"

That is impossible for the default virtual instance. Liferay defines the default
as the company whose `webId` matches the `company.default.web.id` property,
refuses to delete it (`RequiredCompanyException`), and offers no runtime way to
hand the default off to another company (reassigning it needs a property change
and a restart). So a clean instance is achieved by resetting the default
company's data in place instead — which is exactly what this reset does, with no
restart.

## Prerequisite

The Liferay containers must already be running (`docker ps` shows a healthy
`liferay` container). If they are not, start them with `/one-env-up` first.

## Run

```bash
scripts/instance_reset.sh
```

The script pins basic auth against localhost, then:

1. Tears down the seeded records and structural scaffolding
   (`scripts/seed/teardown.sh --full`): records, object definitions,
   relationships, commerce configuration, roles, taxonomies, and list type
   definitions.

1. Deletes the `One` site group through the JSONWS bridge.

1. Redeploys the `liferay-one-batch` client extension and waits for the batch
   engine imports to finish, rebuilding the object definitions and reference
   data.

1. Redeploys the `liferay-one-site-initializer` client extension and waits for
   the `Initialized One for group` log marker, recreating the site.

1. Reseeds the data (`scripts/seed.sh`).

1. Rebinds the `one.localhost` virtual host
   (`scripts/bootstrap/set_virtual_hosts.sh`).

It finishes with `Done. The Liferay instance data has been reset.`

The local-dev OAuth2 application lives in the company and is left untouched, so
there is no need to recreate it afterward.

## Choosing A Reset Tier

- Only the site content or seeded records are stale → `/one-site-reset`.
- The batch-owned scaffolding (objects, roles, taxonomies) needs rebuilding →
  this reset.
- The image, bundle, or database is broken → `/one-env-reset`.