# Integrations

External-system contracts. Each integration owns a single doc describing the direction of data flow, auth, message/payload shape, failure handling, and which workspace code implements it.

All inbound data paths land in `liferay-one-etc-spring-boot`. All outbound calls use typed clients also in `liferay-one-etc-spring-boot`. Credentials live in `liferay-one-instance-settings` secrets.

## Index

| Doc | Direction | Purpose |
|---|---|---|
| [`salesforce.md`](./salesforce.md) | inbound | Sales opportunity → Account / Commerce Order (D12 subscriber) |
| [`jira.md`](./jira.md) | bidirectional | Support ticketing, heat-tag labels, Assets Koroneiki schema, security-vulns read |
| [`gcs.md`](./gcs.md) | bidirectional | Large-file attachments (ticket + publisher asset uploads) |
| [`gcf.md`](./gcf.md) | outbound | Usage metric data for SaaS and composable/PaaS customer environments |
| [`data-warehouse.md`](./data-warehouse.md) | outbound | BigQuery — environment usage metrics (current via GCF); direct access planned for phase 5+ |
| [`liferay-cloud.md`](./liferay-cloud.md) | outbound | Trial + paid portal instance lifecycle (DXP Cloud / Console / LXC) |
| [`analytics-cloud.md`](./analytics-cloud.md) | outbound | Faro workspace provisioning |
| [`nav.md`](./nav.md) | bidirectional | Microsoft NAV (Dynamics 365 BC) — invoice generation, A/R aging events, credit holds |
| [`marketo.md`](./marketo.md) | outbound (client-side) | Marketing form submissions from custom elements |

> **Commerce** is not an external integration — it runs inside the same Liferay instance. Commerce subscription lifecycle events are consumed by Spring beans registered on Liferay's internal event bus (not HTTP). See [`../provisioning-hub.md`](../provisioning-hub.md) for the license-generation and entitlement-sync flows that those events trigger.

## Retired / out of scope

Per system-spec §7.2 and D6/D11/D12:

- **Zendesk** — already retired; vestigial `ZendeskTicketWebService` drops in phase 3.
- **Dossiera** — retires in phase 3; direct SF Pub/Sub subscriber replaces it.
- **osb-entity-web** — retires; Liferay Users are authoritative.
- **RabbitMQ** — internal bus retires; evaluate any remaining outbound Pub/Sub publishers during the phase 6 decommission audit.
- **LCS** — vestigial code drops in phase 3; no new code.

## Auth summary

| External system | Auth | Secrets storage |
|---|---|---|
| Salesforce Pub/Sub | GCP service-account JSON | `liferay-one-instance-settings` secret `gcp-service-account.json` |
| Jira / JSM | API token (Atlassian user-scoped) | `liferay-one-instance-settings` secret `jira-api-token` |
| GCS | GCP service-account JSON | shared with Pub/Sub service account |
| Liferay Cloud | OAuth2 client credentials issued by LXC | `liferay-one-instance-settings` secret `lxc-client-credentials` |
| Analytics Cloud | OAuth2 client credentials | `liferay-one-instance-settings` secret `analytics-cloud-credentials` |
| NAV (Microsoft Dynamics 365 BC) | OAuth2 client credentials (Azure AD) | `liferay-one-instance-settings` secret `nav-client-credentials` |
| Marketo | Client-side Marketo form embed (munchkin token) | `liferay-one-instance-settings` config `marketo-munchkin-id` (non-secret) |
| GCF (customer_usage_api) | GCP service-account ID token | `spring-boot.env` env var `LIFERAY_CUSTOMER_GCF_CUSTOMER_SERVICE_ACCOUNT_KEY` |
| GCF (composable_usage_api) | GCP service-account ID token | `spring-boot.env` env var `LIFERAY_CUSTOMER_GCF_COMPOSABLE_SERVICE_ACCOUNT_KEY` |
| BigQuery (future direct) | GCP service-account JSON | `liferay-one-instance-settings` secret (TBD — see `data-warehouse.md`) |

## Failure handling

Each integration doc specifies its retry strategy. Defaults:

- Inbound subscribers — if the handler throws, the message is nacked and Pub/Sub re-delivers (exponential backoff). After N redeliveries, the message lands in a dead-letter topic (`one-deadletter`) and an alert fires.
- Outbound calls — typed clients retry up to 3 times with exponential backoff for 5xx and timeouts. 4xx fails fast. Failures beyond the retry budget log to a workspace `IntegrationFailure` table (internal Spring Boot, not an Object) and Slack-alert the engineering channel.

## Cross-cutting

- **Correlation IDs.** Every inbound message / outbound call carries a correlation ID, logged in all related writes. For SF Pub/Sub: the opportunity ID. For Jira: the issue key. For Commerce: the order ID.
- **Circuit breakers.** Jira and Liferay Cloud clients wrap calls in circuit breakers (Resilience4j). After N consecutive 5xx, the breaker opens for 60s to avoid cascading.
- **Observability.** Each integration doc lists its metric names and dashboards. All metrics prefix with `one.<integration>.*`.
