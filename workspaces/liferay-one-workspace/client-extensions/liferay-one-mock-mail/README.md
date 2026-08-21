# Liferay One Mock Mail

A throwaway SMTP server for UAT, built on [Mailpit](https://mailpit.axllent.org). It accepts everything Liferay sends, relays nothing, and shows the captured messages in a web inbox. It is the UAT counterpart of the `mail` service that `docker-compose.yaml` already runs locally, so notification templates, invitations, and license key emails can be exercised on UAT without mailing real people.

## Ports

| Port | Protocol | Exposure |
| --- | --- | --- |
| 1025 | SMTP | Cluster internal only. Reachable from the Liferay service at `liferayonemockmail:1025`. |
| 8025 | HTTP | Published by the load balancer. Serves the inbox UI and the Mailpit REST API. |

Both ports have to be declared in the `ports` block of `LCP.json` or the cluster Service exposes neither, and Liferay cannot open an SMTP connection no matter what its mail settings say. Both are declared `"external": false` — only the separate `loadBalancer` block publishes a port to the internet, and it publishes 8025 alone. The SMTP listener therefore stays reachable from inside the environment and nowhere else, so the service cannot be abused as an open relay. `loadBalancer.cdn` is off because a cached inbox would serve stale messages.

The same two-port shape is used by the `maildev` service in `workspaces/liferay-osbfaro-workspace/modules/wedeploy/common/maildev/LCP.json`.

The internal host name is the `id` the workspace writes into the built `LCP.json` — the extension folder name with the hyphens removed, which is `liferayonemockmail`.

## Required Configuration

`MP_UI_AUTH` is mandatory. The inbox and the REST API expose every message the environment has sent, and the load balancer publishes them on a public host name, so the entrypoint refuses to start when neither `MP_UI_AUTH` nor `MP_UI_AUTH_FILE` is set. An unset secret fails the deployment rather than silently opening the mailbox.

Set it on the service in the Liferay Cloud console rather than committing it:

```
MP_UI_AUTH=<username>:<password>
```

Everything else is baked into the `Dockerfile`: a 1000 message cap, any SMTP credentials accepted, and unencrypted SMTP allowed.

## Deploy

`LCP.json` sets `deploy: false` for `extprd` and `infra`, so the service builds and runs on UAT only.

```bash
lcp deploy --project "${PROJECT}-uat"
```

Then confirm the pod is serving:

```bash
lcp log --project "${PROJECT}-uat" --service liferayonemockmail
```

## Point Liferay At It

Deploying the service does not redirect anything on its own — Liferay keeps using whatever mail host the environment already has until its company mail settings change. Pick one of the following.

### Control Panel

The reversible, no-deploy option. In Control Panel go to Configuration and open the mail settings for the instance, then set:

| Field | Value |
| --- | --- |
| Outgoing SMTP Server | `liferayonemockmail` |
| Outgoing Port | `1025` |
| User Name | *(empty)* |
| Password | *(empty)* |
| Use a Secure Network Connection | off |

### An `instanceSettings` Client Extension

The repeatable option, surviving an environment reset. It writes the same `MailSettingCompanyConfiguration` PID that `docker-compose.yaml` overrides locally:

```yaml
liferay-one-mock-mail-instance-settings:
    enableStartTLS: false
    name: Liferay One Mock Mail Instance Settings
    outgoingSMTPPort: "1025"
    outgoingSMTPServer: liferayonemockmail
    pid: com.liferay.mail.settings.configuration.MailSettingCompanyConfiguration
    smtpPassword: ""
    smtpUserName: ""
    type: instanceSettings
```

This block is deliberately not part of `client-extension.yaml`. `deploy: false` keeps the Mailpit *service* off production, but the client extension zip is deployed to DXP through a separate path, and redirecting a production instance's outgoing mail is not a mistake worth risking. Add the block only to an extension whose deploy target is known to be UAT.

`outgoingSMTPPort` is declared as a `String` on the configuration interface, so the quotes matter.

## Verify

Trigger any notification on UAT, then open `https://liferayonemockmail-<project>-uat.lfr.cloud` and sign in with the `MP_UI_AUTH` credentials. The same check from a terminal:

```bash
curl \
	--silent \
	--url "https://liferayonemockmail-<project>-uat.lfr.cloud/api/v1/messages?limit=5" \
	--user "${MP_UI_AUTH}"
```

`/livez` and `/readyz` are exempt from basic authentication, which is why the probes in `LCP.json` can use them.

## Limits

- Messages live in memory. A restart or a redeploy empties the inbox, and the oldest message is dropped once the mailbox reaches 1000. Set `MP_DATABASE` against a mounted volume if a test ever needs the inbox to survive a restart.
- Nothing is forwarded. Every message stops here, which is the point on UAT and the reason the service must never run on production.
- Locally there is nothing to deploy. `docker-compose.yaml` already runs MailDev on SMTP 1025 with its inbox on <http://localhost:1080>.