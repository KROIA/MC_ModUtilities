# Discord Webhooks

Optional Discord webhook output for server events, such as bank transactions or market trades. A mod declares its
message types and posts events; the server admin decides in the config which types go to which Discord webhook.

**Package**: `net.kroia.modutilities.discord`

---

## Prerequisites

- Server-side code only.
- A Discord webhook URL (Discord: channel settings, Integrations, Webhooks) for testing.

---

## Inert until configured

Nothing happens (no thread, no file access) until a mod calls `register`. Registering creates
`config/modutilities/discord/webhooks.json` and `config/modutilities/discord/<modId>.json` if they are missing.
Every message type starts disabled. The sender thread only starts once the admin enables a type and routes it to a valid webhook.

---

## API

| Method | Description |
|---|---|
| `register(String modId, List<String> types)` | Declares the mod's message types and loads (or creates) its config. Call it on the server, e.g. from `LifecycleEvent.SERVER_STARTED`. Calling it again replaces the type list and re-reads all config files |
| `post(String modId, String type, Map<String, String> values)` | Queues one message for every webhook the type is routed to. A cheap no-op if the mod is not registered or the type is disabled or not routed. Never throws and never blocks on the network |
| `reload()` | Re-reads `webhooks.json` and the config of every registered mod |
| `isActive()` | `true` if at least one message type is enabled and routed to a valid webhook |
| `command()` | The `discord reload|status` command node, to attach under your own command root |
| `PERMISSION` | `"modutilities.command.discord"`, the permission node of the command (fallback level 3) |

If `values` contains `"amount"` and it is numeric, its absolute value must be at least the type's `minAmount`, or the event is skipped.

```java
// Server start
LifecycleEvent.SERVER_STARTED.register(server ->
        DiscordWebhooks.register("mymod", List.of("deposit", "withdraw")));

// When the event happens (master or standalone server only)
DiscordWebhooks.post("mymod", "deposit", Map.of(
        "player", playerName,
        "amount", String.valueOf(amount)));

// Command
dispatcher.register(Commands.literal("mymod").then(DiscordWebhooks.command()));
```

The same command is also available as `/modutilities discord reload|status`. Every attachment reloads all config and shows
the same overview of all registered mods.

> [!IMPORTANT]
> `post` does not check the server role. In a [multi-server setup](../networking/MultiServerNetworking.md), post only from the
> master (or a standalone server), otherwise every event is reported once per server. On a slave, `discord status` says so.

---

## Templates

Each mod ships `assets/<modId>/discord/<lang>.json`. If the configured language is missing, `en_us` is used, and without a
template the type key becomes the title.

```json
{
  "deposit": {
    "title": "Deposit",
    "color": 3447003,
    "fields": [
      { "name": "Player", "value": "{player}", "inline": true },
      { "name": "Amount", "value": "{amount}", "inline": true }
    ]
  }
}
```

`{key}` placeholders are replaced with the posted values. A field whose value renders empty is left out.

---

## Configuration (server admins)

`config/modutilities/discord/webhooks.json` names the webhooks. Several mods and types can share one name.
```json
{ "webhooks": { "bank-log": "https://discord.com/api/webhooks/..." } }
```
Only `https` URLs on `discord.com`, `discordapp.com`, `ptb.discord.com` or `canary.discord.com` with a path below
`/api/webhooks/` are accepted. Other entries are skipped with a warning that names the webhook.

`config/modutilities/discord/<modId>.json` turns types on and routes them:
```json
{
  "language": "en_us",
  "types": {
    "deposit": { "enabled": true, "webhooks": ["bank-log"], "minAmount": 1000 }
  }
}
```
A type can also override `title`, `color` and `fields` of its template. New types are added to the file when a mod registers
them; your edits are kept. Run `discord reload` after editing.

---

## Delivery

- Messages are sent from a background thread, batched up to 10 embeds and 6000 characters per Discord message.
- The queue holds up to 1000 messages; when it is full, the oldest message is dropped and counted in `discord status`.
- Discord rate limits (HTTP 429) are respected, and server errors are retried a few times.
- Mentions are disabled, so a posted value cannot ping users or roles.
- On server stop the queue is flushed for up to 5 seconds.

## Security

Webhook URLs contain a secret token. They are never logged, never sent to clients and never shown by `discord status`;
only webhook names appear in output.

---

## See Also

- [Placeholders](Placeholders.md)
- [Multi-Version Compatibility: permission nodes](../development/MultiVersionCompat.md#permission-nodes)
- [Documentation Index](../README.md)
