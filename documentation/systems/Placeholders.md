# Placeholders

Shared placeholder registry. A mod declares its placeholders once through ModUtilities; ModUtilities bridges them to every installed placeholder system. Dependent mods need **no** compile dependency on TAB or Placeholder API.

---

## Prerequisites

- Server-side code only (placeholders are evaluated on the logical server).
- Basic knowledge of `ServerPlayer`.

---

## API

Package `net.kroia.modutilities.placeholder`:

```java
PlaceholderRegistry.registerPlayer(String namespace, String name, int refreshIntervalMs, Function<ServerPlayer, String> function);
PlaceholderRegistry.registerServer(String namespace, String name, int refreshIntervalMs, Supplier<String> supplier);
```

- Call at mod init. Registrations are stored and bridged once a backend becomes available; later registrations are forwarded immediately.
- `namespace` and `name` must match `[a-z0-9_]+`, `refreshIntervalMs` must be `> 0`. Invalid or duplicate registrations log a warning and are ignored.
- A function that throws (or returns `null`) yields an empty string. The first exception per placeholder is logged.

---

## Backends and identifier formats

| Backend | Loaders | Mod ID | Identifier | Refresh interval |
|--:|---|---|---|---|
| NEZNAMY TAB | Fabric, NeoForge | `tab` | `%<namespace>_<name>%` | Passed to TAB |
| Patbox Text Placeholder API | Fabric | `placeholder-api` | `%<namespace>:<name>%` | Ignored (evaluated per request) |

Backends activate only if the mod is loaded. TAB placeholders are registered on server start and re-registered after `/tab reload`.

---

## Thread safety

TAB evaluates placeholders **off the server thread**. Placeholder functions must be thread-safe: read immutable or `volatile`/concurrent data, ideally a value cached by the server thread. Do not touch the world, entities, or non-thread-safe collections inside the function.

---

## Example (dependent mod)

```java
public final class MyModPlaceholders {
    private static volatile String cachedCirculation = "0";

    public static void init() { // call from common mod init
        PlaceholderRegistry.registerPlayer("mymod", "balance", 1000,
                player -> BalanceCache.get(player.getUUID()));          // thread-safe cache
        PlaceholderRegistry.registerServer("mymod", "circulation", 5000,
                () -> cachedCirculation);
    }

    public static void onServerTick() { // server thread updates the cache
        cachedCirculation = computeCirculation();
    }
}
```

Usage: TAB `%mymod_balance%`, Placeholder API `%mymod:balance%`.

---

## See Also

- [Sandbox System](../development/Sandbox.md) - registers `modutilities:sandbox_time` and `modutilities:sandbox_player` when dev features are enabled
- [Utility Classes](Utilities.md)
