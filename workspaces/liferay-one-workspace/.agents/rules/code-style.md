# Code Style

These rules apply to all code in this workspace. Brian Chan enforces them during PR review — violations are rejected or corrected with a follow-up commit.

## Sort Everything

Lists, arrays, and JSON entries must always be in sorted order. This applies to:

- JSON array items in site initializer files (sort by `externalReferenceCode` or `key`)
- `[#assign ... /]` variable blocks in FreeMarker templates (logical dependency order, then alphabetical)
- Java `import` statements (already handled by source formatter)
- Entries in configuration files
- Method and constructor parameter lists — alphabetical by parameter name. Reordering a signature means reordering the arguments at every call site. Where the reordered parameters share a type, the compiler cannot catch a missed call site — the code still compiles while the arguments land in the wrong parameters — so verify each call site by reading it against the new order, not by trusting a green build. Example (same type, the dangerous case): `notify(String message, String recipient)` → `notify(String recipient, String message)`.

When Brian sees items out of order, he comments `"sort"` and bounces the PR back.

## Log Message Conventions

Do not write log statements like AI-generated code. Specifically:

- Error messages: use `"Unable to <verb>"` not `"Error <verb>ing"` or `"Error: <noun>"`
- Product/object names in log strings: no hyphens — write `"business event"` not `"business-event"`, `"business events"` not `"business-events"`

Example of what Brian corrected:

```java
// Wrong — AI slop
_log.info("GET business-events for " + externalReferenceCode);
_log.error("Error updating business event " + id);

// Correct
_log.info("GET business events for " + externalReferenceCode);
_log.error("Unable to update business event " + id);
```

## User-Facing Text

- Use "IDs" (not "Id", "id", "codes", or other terms) when referring to identifier values shown to users. This includes object field `label` values in batch definitions — write `"Catalog External ID"`, not `"Catalog External Id"`.
- Semantic precision matters: "Email" and "Email Address" are different — don't add "Address" if the field is just an email

## FreeMarker Variable Blocks

In FreeMarker templates (`.ftl`, `index.html`), group all `[#assign ... /]` statements together in a single block at the top. Within the block, order variables by dependency (assign prerequisites before their dependents).

```freemarker
[#assign
    currentFriendlyURL = ... /]
[#assign
    currentURL = ... /]
```

→ Both should be in one logical block, with `currentFriendlyURL` before `currentURL` since `currentURL` may depend on it.

### Reset per-iteration state inside `[#list]`

`[#assign]` variables are template-scoped, not iteration-scoped — a value assigned in one `[#list]` pass persists into the next. When a variable is assigned only inside a guard (`[#if x?has_content]`), a later iteration whose guard is false still reads the *previous* iteration's value, so the wrong data renders. Reset every such variable to a neutral default at the top of the loop body, before the guards.

```freemarker
[#-- Wrong: principalCategory leaks from the previous product --]
[#list products as product]
    [#if product.categories?has_content]
        [#assign principalCategory = product.categories[0] /]
    [/#if]
    [#if principalCategory?has_content]...[/#if]
[/#list]

[#-- Correct: reset before the guard --]
[#list products as product]
    [#assign principalCategory = "" /]
    [#if product.categories?has_content]
        [#assign principalCategory = product.categories[0] /]
    [/#if]
    [#if principalCategory?has_content]...[/#if]
[/#list]
```

## Image URLs in Fragments

Never string-replace `https://` to `http://` on an image or document URL in shipped fragment markup (`src="${imageURL?replace('https://', 'http://')}"`). This is a local-dev bandaid: on any HTTPS environment (UAT/prod) it forces the asset to `http://`, which the browser blocks as mixed content, and the image silently fails to load. Serve the asset over the current scheme instead of rewriting the protocol.

## Date Input Values Are Timezone-Naive

Never feed a `yyyy-MM-dd` value from an `<input type="date">` straight into `new Date(...).toISOString()`. A bare date string is parsed as **UTC midnight**, so in any UTC-negative timezone (all of the Americas) `.toISOString()` and any later local-time display shift the day backward by one — the saved start/expiration date is off by one from what the user picked.

```ts
// Wrong — 2026-03-15 selected in the US saves/renders as 2026-03-14
function toISODate(value?: string): string | undefined {
	return value ? new Date(value).toISOString() : undefined;
}

// Correct — pin to local noon so the calendar day survives any offset
function toISODate(value?: string): string | undefined {
	return value ? new Date(`${value}T12:00:00`).toISOString() : undefined;
}
```

Prefer a shared helper in `~/utils/dateUtils.ts` over re-deriving this per file.

## Java Code Ordering

In Java classes, declare resource clients before the objects that use them:

```java
// Wrong
Account account = new Account();
account.setName(...);
AccountResource accountResource = AccountResource.builder()...build();

// Correct
AccountResource accountResource = AccountResource.builder()...build();
Account account = new Account();
account.setName(...);
```