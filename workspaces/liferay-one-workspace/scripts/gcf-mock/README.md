# DataOps Metrics API Mock

Fakes the Liferay Data Platform Metrics API so the LDP usage endpoints on
`liferay-one-etc-spring-boot` can be exercised in the local k3s harness, without GCP
credentials and without a deployed Cloud Function.

## Why This Is Needed

`GoogleCloudFunctionService` authenticates before it makes a request. `_getIdTokenProvider`
calls `(IdTokenProvider)GoogleCredentials.getApplicationDefault()`, so with no application
default credentials present it throws `GoogleCloudFunctionUnavailableException`,
`ProjectRestController` logs "Unable to reach the DataOps usage API" and returns null, and
the request never leaves the pod. Stubbing HTTP alone therefore changes nothing.

The way through is `GCE_METADATA_HOST`. Both `DefaultCredentialsProvider` and
`ComputeEngineCredentials` honor it, so pointing it at the mock makes the credential chain
resolve to `ComputeEngineCredentials`, which fetches an ID token over plain unauthenticated
HTTP. The mock serves that token alongside the usage endpoints.

Two details are easy to get wrong:

1. The availability probe requires a `Metadata-Flavor: Google` response header.

1. `IdTokenCredentials.refresh` parses the returned token as a JWT to read `exp`, so an
opaque string fails. The `Caddyfile` serves a well-formed unsigned JWT whose signature is
never verified.

## Payload Provenance

The response bodies follow the **0.1.1 - Liferay Data Platform Metrics API Contract** page in
the Data Operations Confluence space, linked from DOPS-3607. The counts are made up; the
field names, nesting, and status codes are the documented contract. Update the `Caddyfile`
whenever that contract changes.

## How It Is Wired

The mock is the `liferay-one-gcf-mock` client extension, so the normal workspace flow builds
and deploys it. Its `Dockerfile` runs `mock/generate-fixtures.sh` during the build and Caddy serves the
result, so the served responses are generated at image build time and never committed. The
Caddy image already carries bash, so no extra build stage is needed.

Redirecting the DataOps calls at it takes two overrides:

```
GCE_METADATA_HOST=<mock host>:80
LIFERAY_ONE_GCF_BASE_URL=http://<mock host>
```

`configure-local.sh` writes them to the gitignored root `.env.local`, which is the channel
the `one-deploy` skill documents: it is read after `build/local.env`, so it wins. It resolves
the host itself, which differs between the two environments.

```bash
./configure-local.sh          # then rebuild or recreate the environment
./verify.sh
./configure-local.sh --remove
```

## Docker Compose

`.env.local` is already listed as a second `env_file` on `liferay-one-etc-spring-boot`, so the
overrides apply as soon as the container is recreated. Compose does not run container client
extensions at all -- there is no kong service there either -- so `configure-local.sh` also
declares the mock as a service in the gitignored `docker-compose.override.yaml`.

The client extension runs with `network_mode: service:liferay`, sharing the portal's network
namespace, which is also why `.serviceAddress: localhost:8080` works there. That sharing means
it inherits compose's resolver, so the mock resolves by service name, and the client extension
publishes 58081 on the portal container, so `verify.sh` reaches it from the host.

## k3s

Only relevant if you run the LEC k3s harness rather than docker compose. Everything above
applies unchanged; two things are specific to it.

The recipe deploys any directory carrying an `LCP.json`, so the client extension needs nothing
extra, but it names the Deployment and Service after the `LCP.json` id, which the workspace
build rewrites to the client extension name with the dashes stripped. The mock is therefore
`liferayonegcfmock` here and `liferay-one-gcf-mock` under compose, which is why
`configure-local.sh` resolves the host from whichever backend is running instead of writing a
fixed name. Run it before building the environment: the pod environment is assembled as the
environment comes up, so a value written afterwards does not reach a pod that is already
running.

Reaching a Service by name also needs the `br_netfilter` kernel module on the host. Pods sit
on a CNI bridge, and bridged traffic bypasses netfilter unless that module is loaded, so
kube-proxy's ClusterIP rules never apply. A request to a Service address then times out while
the pod IP still works and names still resolve, which makes it look like a name resolution
problem when it is not. It is a documented Kubernetes prerequisite that most installers
arrange, and k3s in Docker shares the host kernel, so the host has to load it:

```bash
sudo modprobe br_netfilter
sudo sysctl -w net.bridge.bridge-nf-call-iptables=1
```

Persist it through `/etc/modules-load.d/` and `/etc/sysctl.d/` to survive a reboot. Without it
every cross client extension call in this harness fails the same way, as a hang rather than an
error.

## Editing The Data

All the numbers live in a data block at the top of `mock/generate-fixtures.sh`: the ten
monthly event buckets as `MONTHS` rows of month, Liferay count and Salesforce count, then the
four summary metrics and the project identifiers. The script derives every served response
from that block, so the per-month `event-summary-*.json` files and the nested buckets inside
`event-history.json` cannot drift apart -- each month's counts would otherwise have to be
written twice.

A month left out of `MONTHS` returns zero counts from event-summary and no bucket from
event-history, which is how the deliberate 2026-01 and 2026-04 gaps work.

Entitlement caps are deliberately **not** here. Those come from the seeded orders, so the
percentages the UI shows are mock usage over real entitlements.

Rebuild the client extension image after editing.

## This Never Reaches A Deployed Environment

`LCP.json` carries a top level `"deploy": false`, so Liferay Cloud skips the client extension
in every environment. It is a single switch rather than a list of environment names, so a new
environment cannot pick the mock up by accident.

It still deploys locally because the local recipe never reads that field. It reads only `id`,
`kind`, `env`, `dependencies`, `livenessProbe`, `readinessProbe`, `scale`, `schedule` and
`loadBalancer`, discovering client extensions by the presence of an `LCP.json` at all.

Nothing points the client extension at the mock on its own either. That takes the two
overrides in `.env.local`, which `configure-local.sh` writes and `--remove` takes back out.