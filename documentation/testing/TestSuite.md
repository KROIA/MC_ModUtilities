# Test Suite Framework

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Architecture](#architecture)
4. [API Reference](#api-reference)
   - [TestResult](#testresult)
   - [TestCategory](#testcategory)
   - [TestSuite](#testsuite)
   - [TestRunner](#testrunner)
   - [TestRegistry](#testregistry)
   - [TestCommandRegistration](#testcommandregistration)
   - [LuckPermsPhase](#luckpermsphase)
5. [Writing Tests](#writing-tests)
6. [Defining Categories](#defining-categories)
7. [Integrating in Your Mod](#integrating-in-your-mod)
8. [Commands](#commands)
9. [Dev Builds and Release Builds](#dev-builds-and-release-builds)
10. [Offline Tests (JUnit)](#offline-tests-junit)
11. [ModUtilities Internal Tests](#modutilities-internal-tests)

---

## Overview

The MC_ModUtilities Test Suite Framework provides **in-game unit testing** that runs via Minecraft chat commands. It is designed to:

- **Test mod logic inside a live Minecraft server** without external tooling.
- **Be shared across multiple mods** (ModUtilities, BankSystem, StockMarket, and any future mod) through a single framework living in the `net.kroia.modutilities.testing` package.
- **Filter tests by server role** in a master/slave multi-server setup, so master-only tests do not run on slave servers and vice versa.
- **Stay out of release builds entirely**: the `testing` package is only compiled when the Gradle property `enable_dev_features` is `true` (see [Dev Builds and Release Builds](#dev-builds-and-release-builds)).
- **Run automatically** without a player: start the game with `-Dmodutilities.autotest=<label>` (see [Automated runs](#automated-runs)).

All test output is rendered as color-coded chat messages directly to the player who triggered the command, with green checkmarks for passing tests, red crosses for failures, and yellow exclamation marks for errors.

### Package

```
net.kroia.modutilities.testing
```

---

## Quick Start

Follow these four steps to add a test to any mod that depends on MC_ModUtilities.

### Step 1: Define a Category

```java
package com.example.mymod.testing;

import net.kroia.modutilities.testing.TestCategory;

public class MyModTestCategories {
    public static final TestCategory CORE = new TestCategory(
        "mymod_core",                    // unique name (prefix with mod abbreviation)
        "Core logic tests for MyMod",    // human-readable description
        TestCategory.ServerType.BOTH,    // run on master, slave, or both
        false                            // true if tests need a MinecraftServer instance
    );
}
```

### Step 2: Create a Test Suite

```java
package com.example.mymod.testing;

import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;

public class CoreTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return MyModTestCategories.CORE;
    }

    @Override
    public void registerTests() {
        addTest("addition_works", this::testAddition);
        addTest("string_not_null", this::testStringNotNull);
    }

    private TestResult testAddition() {
        return assertEquals("1 + 1 should be 2", 2, 1 + 1);
    }

    private TestResult testStringNotNull() {
        String value = "hello";
        return assertNotNull("Value should not be null", value);
    }
}
```

### Step 3: Register It

During mod initialization:

```java
import net.kroia.modutilities.testing.TestRegistry;

public class MyMod {
    public static void init() {
        // Only reached in dev builds (enable_dev_features=true)
        TestRegistry.register(new CoreTests());
    }
}
```

### Step 4: Register the Command and Run

In your command registration callback:

```java
import net.kroia.modutilities.testing.TestCommandRegistration;

// Inside your command registration handler:
TestCommandRegistration.register(dispatcher, "mymod", "MyMod", isSlave);
```

Then in-game, run:

```
/mymod test           -- runs all tests
/mymod test list      -- lists available categories
/mymod test mymod_core -- runs only the mymod_core category
```

---

## Architecture

### Component Diagram

```
TestCommandRegistration          (wires Brigadier commands)
        |
        v
    TestRunner                   (orchestrates execution, formats output)
        |
        +---> TestRegistry       (static list of all registered TestSuites)
        |         |
        |         v
        |     TestSuite[]        (one or more per mod)
        |         |
        |         +---> TestCategory   (metadata: name, server type, context flag)
        |         +---> Map<String, Supplier<TestResult>>  (named test methods)
        |
        v
    TestResult                   (PASS / FAIL / ERROR with details)
```

### Execution Flow

1. **Registration** -- During mod init, each mod calls `TestRegistry.register(suite)` for every `TestSuite` it wants to expose. Simultaneously, `TestCategory` objects are created as static fields, which self-register into a global category list.

2. **Command** -- A player with permission level 2+ runs a `/modname test` command. `TestCommandRegistration` dispatches this to a new `TestRunner` instance.

3. **Runner** -- `TestRunner` iterates over registered suites. For each suite whose category can run on the current server type:
   - Calls `suite.registerTests()` (lazy, only on first run).
   - Calls `suite.setServer(server)` to inject the `MinecraftServer` instance.
   - Calls `suite.setup()`.
   - Executes each test function, collecting `TestResult` objects.
   - Calls `suite.teardown()`.
   - Reports individual results and per-suite summaries to the player chat.

4. **Results** -- After all suites complete, a final summary is printed showing total passed, failed, error, and skipped counts.

---

## API Reference

### TestResult

```
net.kroia.modutilities.testing.TestResult
```

An immutable value object representing the outcome of a single test.

#### Status Enum

| Value   | Meaning                                            |
|---------|----------------------------------------------------|
| `PASS`  | The test assertion succeeded.                      |
| `FAIL`  | The test assertion did not hold.                   |
| `ERROR` | An unexpected exception occurred during execution. |
| `SKIP`  | The test did not run because a precondition is missing (e.g. an optional mod). Counted as skipped, not failed. |

#### Static Factory Methods

| Signature | Description |
|-----------|-------------|
| `TestResult.pass(String testName, @Nullable String message)` | Create a passing result. |
| `TestResult.fail(String testName, @Nullable String message)` | Create a failure with a message only. |
| `TestResult.fail(String testName, @Nullable String message, String expected, String actual)` | Create a failure with expected/actual values for diff display. |
| `TestResult.error(String testName, String message)` | Create an error result (uncaught exception, setup failure, etc.). |
| `TestResult.skip(String testName, String reason)` | Create a skipped result. Inside a suite, use the protected `skip(reason)` helper. |

#### Instance Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getTestName()` | `String` | The name of the test. |
| `getStatus()` | `Status` | `PASS`, `FAIL`, `ERROR` or `SKIP`. |
| `passed()` | `boolean` | Convenience: `true` if status is `PASS`. |
| `getMessage()` | `@Nullable String` | Descriptive message (present on all statuses). |
| `getExpected()` | `@Nullable String` | Expected value string (only on assertion failures). |
| `getActual()` | `@Nullable String` | Actual value string (only on assertion failures). |

---

### TestCategory

```
net.kroia.modutilities.testing.TestCategory
```

Metadata describing a group of related tests. Categories self-register into a global list on construction.

#### ServerType Enum

| Value          | Tests run on...              |
|----------------|------------------------------|
| `MASTER_ONLY`  | Only the master server.      |
| `SLAVE_ONLY`   | Only slave servers.          |
| `BOTH`         | Both master and slave.       |

#### Constructor

```java
public TestCategory(
    String name,                   // unique identifier, e.g. "mu_events"
    String description,            // human-readable summary
    ServerType serverType,         // where this category can execute
    boolean needsMinecraftContext  // true if tests require a server instance
)
```

The constructor automatically adds the new category to the global `ALL_CATEGORIES` list.

#### Instance Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getName()` | `String` | The unique category name. |
| `getDescription()` | `String` | Human-readable description. |
| `getServerType()` | `ServerType` | Which server types can run this category. |
| `needsMinecraftContext()` | `boolean` | Whether the tests need a `MinecraftServer`. |
| `canRunOn(boolean isSlave)` | `boolean` | Returns `true` if this category is allowed to execute on the given server type. |

#### Static Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `fromName(String name)` | `@Nullable TestCategory` | Look up a category by name (case-insensitive). Returns `null` if not found. |
| `getAllCategories()` | `List<TestCategory>` | Unmodifiable list of all registered categories. |

---

### TestSuite

```
net.kroia.modutilities.testing.TestSuite
```

Abstract base class that all test suites extend. Provides test registration, lifecycle hooks, server access, and assertion helpers.

#### Abstract Methods (Must Override)

| Method | Description |
|--------|-------------|
| `TestCategory getCategory()` | Return the `TestCategory` this suite belongs to. |
| `void registerTests()` | Called once (lazily) to register test methods via `addTest()`. |

#### Lifecycle Hooks (Optional Override)

| Method | Description |
|--------|-------------|
| `void setup()` | Called before any test in the suite runs. Use for shared initialization. |
| `void teardown()` | Called after all tests in the suite complete. Use for cleanup. |
| `boolean needsGame()` | Default `true`. Return `false` if the suite also runs without Minecraft, in the [offline JUnit run](#offline-tests-junit). Override it only after the suite passed there. |

#### Protected Methods

| Method | Description |
|--------|-------------|
| `addTest(String name, Supplier<TestResult> test)` | Register a named test. Call this inside `registerTests()`. |
| `@Nullable MinecraftServer getServer()` | Access the server instance (available after `setServer()` is called by the runner). `null` in the offline run. |
| `TestResult skip(String reason)` | Return this from a test whose precondition is missing (e.g. an optional mod is not installed). |
| `<T> T await(CompletableFuture<T> future, long timeoutMillis)` | Waits for the future and returns its result. On the server thread it keeps running queued server tasks meanwhile, so work the future needs on the server thread cannot deadlock against the waiting test. Throws `IllegalStateException` on timeout; a failed future rethrows its cause wrapped. |
| `boolean pumpUntil(BooleanSupplier done, long timeoutMillis)` | Runs queued server tasks until `done` is true or the timeout passed. Returns `done` at the end. |
| `void pump(long millis)` | Runs queued server tasks for `millis`, so async callbacks that hop to the server thread can arrive. |

#### Public Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getTests()` | `Map<String, Supplier<TestResult>>` | All registered tests (insertion-ordered). |
| `getTestCount()` | `int` | Number of registered tests. |
| `setServer(@Nullable MinecraftServer server)` | `void` | Called by `TestRunner` before execution. |

#### Assertion Helpers

All assertion methods are `protected` and return a `TestResult`. They are designed to be returned directly from test methods. See the [Assertion Helpers Reference Table](#assertion-helpers-reference-table) in the Writing Tests section for the complete list.

---

### TestRunner

```
net.kroia.modutilities.testing.TestRunner
```

Orchestrates test execution and reports results to a player via chat messages.

#### Constructor

```java
public TestRunner(String modName, String modId, boolean isSlave, @Nullable MinecraftServer server)
```

| Parameter | Description |
|-----------|-------------|
| `modName` | Displayed in the chat prefix, e.g. `[MyMod Test]`. |
| `modId`   | Selects this mod's suites from `TestRegistry` and names the report file `logs/UnitTests/<modId>.log`. |
| `isSlave` | Whether the current server is a slave instance (used for category filtering). |
| `server`  | The `MinecraftServer` instance, passed to suites that need it. |

#### Public Methods

| Method | Description |
|--------|-------------|
| `runAll(@Nullable ServerPlayer player)` | Run all registered test suites that are eligible for the current server type. Skips suites whose category `canRunOn(isSlave)` returns `false`, suites deselected by the [test selection](#test-selection) and suites that run in the offline run instead. |
| `runCategory(@Nullable ServerPlayer player, String categoryName)` | Run only suites matching the given category name. Reports an error if the category is unknown or not eligible. |
| `listCategories(ServerPlayer player)` | Print all known categories to chat, showing test counts. Ineligible categories are shown with strikethrough text and a reason. |
| `getResults()` | Per-suite results of every suite run so far, keyed by `category/SuiteClass`. Used by the `AutoTest` hook for its JSON report. |

**Headless runs and failure reporting:**

- `player` may be `null`: the run is headless (no chat output) and all results are still collected in `getResults()` and written to `logs/UnitTests/<modId>.log`. The `AutoTest` hook runs suites this way.
- If a suite's `setup()` or `teardown()` throws, the failure is recorded as an `ERROR` result named `<setup>` / `<teardown>` instead of being lost. `teardown()` always runs (from a `finally` block), also after a failed `setup()`, and `Throwable`s (not just `Exception`s) are caught.

#### Chat Output Format

- **Per-test results** are indented with two spaces:
  - `  ✓ test_name` (green) for passing tests.
  - `  ✗ test_name` (red) for failures, followed by `Expected: X Got: Y` if available.
  - `  ! test_name` (yellow) for errors, followed by the error message.
  - `  - test_name (skipped: reason)` (dark gray) for skipped tests.
- **Per-suite summaries** show `category_name: N/M passed, F failed, E errors`, plus `S skipped` if any test was skipped.
- **Final summary** shows total counts across all suites.

---

### TestRegistry

```
net.kroia.modutilities.testing.TestRegistry
```

Static registry that holds all test suites across all mods. Also contains the master kill switch.

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `ENABLE_TESTS` | `public static final boolean` | Legacy switch, always `true`. Release builds exclude the whole `testing` package instead; see [Dev Builds and Release Builds](#dev-builds-and-release-builds). |
| `EXCLUDE_PROPERTY` | `String` | `"modutilities.tests.exclude"`, see [Test selection](#test-selection). |
| `INCLUDE_PROPERTY` | `String` | `"modutilities.tests.include"`, see [Test selection](#test-selection). |
| `OFFLINE_PROPERTY` | `String` | `"modutilities.tests.offline"`, see [Test selection](#test-selection). |

#### Static Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `register(TestSuite suite)` | `void` | Add a suite to the registry. No-op if `ENABLE_TESTS` is `false`. |
| `getTestSuites()` | `List<TestSuite>` | Unmodifiable list of all registered suites. |
| `getAvailableCategories(boolean isSlave)` | `List<String>` | Distinct category names whose `canRunOn(isSlave)` returns `true`. Used for command tab-completion. |
| `clear()` | `void` | Remove all registered suites. Useful for test reloading scenarios. |
| `topicKey(TestSuite suite)` | `String` | `<modid>/<Category>/<SuiteClass>`, the key the selection properties match. |
| `isExcluded(TestSuite suite)` | `boolean` | `true` if the include/exclude properties deselect the suite. |
| `isOffline(TestSuite suite)` | `boolean` | `true` if the suite has `needsGame() == false` and matches the offline property. |

---

### TestCommandRegistration

```
net.kroia.modutilities.testing.TestCommandRegistration
```

Wires the test framework into Minecraft's Brigadier command system.

#### Static Methods

```java
public static void register(
    CommandDispatcher<CommandSourceStack> dispatcher,
    String commandRoot,   // e.g. "mymod"
    String modName,       // e.g. "MyMod" (display name for chat prefix)
    boolean isSlave       // whether this is a slave server
)
```

This single call registers three sub-commands under `/<commandRoot> test`. It is a no-op if `TestRegistry.ENABLE_TESTS` is `false`.

All commands require **permission level 2** (operator).

The `category` argument provides **tab-completion** by querying `TestRegistry.getAvailableCategories(isSlave)`.

---

### LuckPermsPhase

```
net.kroia.modutilities.testing.LuckPermsPhase
```

Helper for testing a world after LuckPerms was removed from it. The scenario has two phases on the same world:

1. **Phase 1, with LuckPerms:** tests `record(...)` what must survive, e.g. "player X has the admin flag".
2. **Phase 2, the same world without LuckPerms** (`isPhase2(server)`): tests read the values back with `expected(...)` /
   `expectations(...)` and check that the mod still works and the state is intact.

| Method | Description |
|--------|-------------|
| `FILE` | `"testing/luckperms_phase1.json"`, the phase file relative to the world folder. |
| `file(MinecraftServer)` | Absolute path of the phase file. |
| `phase1Exists(MinecraftServer)` | `true` if the world has a phase file. |
| `isPhase2(MinecraftServer)` | `true` if the world has a phase file and LuckPerms is not installed. |
| `record(MinecraftServer, String modId, String key, String value)` | Stores the value for the mod. Writes only while LuckPerms is available and returns `false` otherwise, so runs without LuckPerms never create the file. A key recorded again overwrites the old value. |
| `expected(MinecraftServer, String modId, String key)` | The recorded value, or `null`. |
| `expectations(MinecraftServer, String modId)` | Everything the mod recorded (empty if nothing). |

The file layout is `{"mods": {"<modId>": {"<key>": "<value>", ...}}}`; keys and values are plain strings.

---

## Writing Tests

### Assertion Helpers Reference Table

All assertion helpers are `protected` methods on `TestSuite` and return `TestResult`. Return them directly from your test methods.

| Method | Signature | Description |
|--------|-----------|-------------|
| `pass` | `pass(String message)` | Explicitly pass with a message. |
| `fail` | `fail(String message)` | Explicitly fail with a message. |
| `assertEquals` | `assertEquals(Object expected, Object actual)` | Fail if `expected` does not equal `actual`. Handles `null` values. |
| `assertEquals` | `assertEquals(String message, Object expected, Object actual)` | Same, with a custom message. |
| `assertTrue` | `assertTrue(boolean condition)` | Fail if condition is `false`. |
| `assertTrue` | `assertTrue(String message, boolean condition)` | Same, with a custom message. |
| `assertFalse` | `assertFalse(boolean condition)` | Fail if condition is `true`. |
| `assertFalse` | `assertFalse(String message, boolean condition)` | Same, with a custom message. |
| `assertNotNull` | `assertNotNull(Object object)` | Fail if object is `null`. |
| `assertNotNull` | `assertNotNull(String message, Object object)` | Same, with a custom message. |
| `assertNull` | `assertNull(Object object)` | Fail if object is not `null`. |
| `assertNull` | `assertNull(String message, Object object)` | Same, with a custom message. |
| `assertThrows` | `assertThrows(Class<? extends Throwable> expectedType, Runnable runnable)` | Fail if the runnable does not throw the expected exception type. |
| `assertThrows` | `assertThrows(String message, Class<? extends Throwable> expectedType, Runnable runnable)` | Same, with a custom message. |

### setup() and teardown() Lifecycle

Override `setup()` to run shared initialization before any test in the suite. Override `teardown()` to clean up after all tests complete.

```java
public class MyTests extends TestSuite {
    private SomeService service;

    @Override
    public void setup() {
        service = new SomeService();
        service.initialize();
    }

    @Override
    public void teardown() {
        service.shutdown();
        service = null;
    }

    @Override
    public void registerTests() {
        addTest("service_is_running", () -> assertTrue("Service should be active", service.isActive()));
    }

    // ...
}
```

If `setup()` throws an exception, all tests in the suite are counted as errors and none are executed. If `teardown()` throws, a warning is printed but test results are preserved.

### Server Context Access

For tests that need the Minecraft server (e.g. to access levels, player data, or registries), use `getServer()`:

```java
private TestResult testServerExists() {
    MinecraftServer server = getServer();
    return assertNotNull("Server should be available", server);
}
```

The server instance is injected by `TestRunner` before `setup()` is called. Set `needsMinecraftContext = true` on your `TestCategory` to document this requirement.

### Common Patterns

**Testing with mutable state:**

```java
private TestResult testCounterIncrements() {
    Counter counter = new Counter();
    counter.increment();
    counter.increment();
    return assertEquals("Counter should be 2 after two increments", 2, counter.getValue());
}
```

**Testing exceptions:**

```java
private TestResult testDivisionByZero() {
    return assertThrows(
        "Dividing by zero should throw ArithmeticException",
        ArithmeticException.class,
        () -> { int x = 1 / 0; }
    );
}
```

**Testing null handling:**

```java
private TestResult testLookupReturnsNullForMissing() {
    MyMap map = new MyMap();
    return assertNull("Missing key should return null", map.get("nonexistent"));
}
```

### What Makes a Good Test Name

- Use **snake_case** to match the convention in existing tests (e.g. `signal_fires`, `data_event_carries_data`).
- Start with the **thing being tested**, then describe the **expected behavior**.
- Keep names concise but descriptive -- they appear directly in chat output.
- Examples: `account_balance_starts_at_zero`, `transfer_fails_for_negative_amount`, `signal_remove_listener`.

---

## Defining Categories

### Creating a Category

Each category is a `static final` field on a categories class. The constructor auto-registers it into the global list.

```java
public class MyModTestCategories {
    public static final TestCategory DATABASE = new TestCategory(
        "mymod_database",
        "Database CRUD tests",
        TestCategory.ServerType.MASTER_ONLY,
        true
    );
}
```

### ServerType Explained

| ServerType     | Use when...                                                       |
|----------------|-------------------------------------------------------------------|
| `MASTER_ONLY`  | Tests touch master-side state (persistence, settings, databases). |
| `SLAVE_ONLY`   | Tests validate slave-specific behavior (replication, sync).       |
| `BOTH`         | Tests are server-role agnostic (pure logic, events, utilities).   |

The `TestRunner` uses `category.canRunOn(isSlave)` to decide whether to execute or skip a suite. Skipped tests contribute to the "skipped" count in the summary.

### needsMinecraftContext Flag

Set this to `true` when your tests call `getServer()` and need a live `MinecraftServer` instance. This is a documentation/metadata flag -- the framework always injects the server regardless, but the flag communicates intent to other developers and can be used by tooling in the future.

### Naming Conventions

- **Prefix with a mod abbreviation** to avoid collisions: `mu_` for ModUtilities, `bs_` for BankSystem, `sm_` for StockMarket.
- Use **snake_case** for consistency.
- Keep names short enough to be comfortable as command arguments (they are typed in chat).

### Registration via Static Fields

Categories register themselves when their containing class is loaded. Ensure the categories class is referenced (e.g. by a `TestSuite.getCategory()` return) so the JVM loads it. Static fields are initialized in declaration order.

---

## Integrating in Your Mod

This section walks through adding the test framework to a mod that depends on MC_ModUtilities. We use a hypothetical "BankSystem" mod as the example.

### Step 1: Create a Categories File

```java
package net.kroia.banksystem.testing.categories;

import net.kroia.modutilities.testing.TestCategory;

public class BankSystemTestCategories {
    public static final TestCategory ACCOUNTS = new TestCategory(
        "bs_accounts", "Account creation and lookup tests",
        TestCategory.ServerType.MASTER_ONLY, true);

    public static final TestCategory TRANSACTIONS = new TestCategory(
        "bs_transactions", "Deposit, withdraw, and transfer tests",
        TestCategory.ServerType.MASTER_ONLY, true);

    public static final TestCategory SYNC = new TestCategory(
        "bs_sync", "Master/slave balance synchronization",
        TestCategory.ServerType.BOTH, true);
}
```

### Step 2: Create Test Suites

```java
package net.kroia.banksystem.testing.tests;

import net.kroia.modutilities.testing.TestCategory;
import net.kroia.modutilities.testing.TestResult;
import net.kroia.modutilities.testing.TestSuite;
import net.kroia.banksystem.testing.categories.BankSystemTestCategories;

public class AccountTests extends TestSuite {

    @Override
    public TestCategory getCategory() {
        return BankSystemTestCategories.ACCOUNTS;
    }

    @Override
    public void registerTests() {
        addTest("create_account", this::testCreateAccount);
        addTest("duplicate_account_fails", this::testDuplicateAccount);
        addTest("lookup_missing_returns_null", this::testLookupMissing);
    }

    private TestResult testCreateAccount() {
        // ... your test logic here
        return assertTrue("Account should be created", true);
    }

    private TestResult testDuplicateAccount() {
        return assertThrows(
            "Creating duplicate account should throw",
            IllegalStateException.class,
            () -> { /* ... */ }
        );
    }

    private TestResult testLookupMissing() {
        return assertNull("Missing account should be null", null);
    }
}
```

### Step 3: Register Suites During Mod Init

```java
package net.kroia.banksystem;

import net.kroia.modutilities.testing.TestRegistry;
import net.kroia.banksystem.testing.tests.AccountTests;
import net.kroia.banksystem.testing.tests.TransactionTests;

public class BankSystemMod {
    public static void init() {
        // ... other initialization ...

        TestRegistry.register(new AccountTests());
        TestRegistry.register(new TransactionTests());
        // register more suites as needed
    }
}
```

### Step 4: Register Commands

In your mod's command registration hook (varies by mod loader):

```java
import net.kroia.modutilities.testing.TestCommandRegistration;

public class BankSystemCommandHandler {
    public static void registerCommands(
            CommandDispatcher<CommandSourceStack> dispatcher, boolean isSlave) {
        // ... other commands ...

        TestCommandRegistration.register(dispatcher, "banksystem", "BankSystem", isSlave);
    }
}
```

### Result

Players with operator permissions can now run:

```
/banksystem test             -- run all BankSystem tests
/banksystem test list        -- list bs_accounts, bs_transactions, bs_sync
/banksystem test bs_accounts -- run only account tests
```

---

## Commands

All test commands require **operator permission level 2**.

### Command Structure

```
/<commandRoot> test [subcommand]
```

Where `<commandRoot>` is the string passed to `TestCommandRegistration.register()`.

### Sub-commands

| Command | Description |
|---------|-------------|
| `/<root> test` | Run **all** registered test suites eligible for this server type. |
| `/<root> test list` | List all known categories with test counts, descriptions, and eligibility. |
| `/<root> test <category>` | Run only suites matching the given category name. Tab-completion is supported. |

### Expected Chat Output

**Running all tests:**

```
[MyMod Test] Running: mu_events (10 tests)
  ✓ signal_fires
  ✓ signal_multiple_listeners
  ✗ signal_remove_listener
    Expected: 0 Got: 1
  ...
[MyMod Test] mu_events: 9/10 passed, 1 failed

[MyMod Test] === Summary ===
[MyMod Test] 9 passed, 1 failed, 3 skipped
```

**Listing categories:**

```
[MyMod Test] Available test categories:
  mu_events (10 tests) - DataEvent and Signal tests
  mu_networking (5 tests) - NetworkPacketManager and ARRS tests
  mu_settings (3 tests) - ModSettings save/load tests [slave only]
```

Categories that cannot run on the current server type are shown with strikethrough formatting and a reason label (`[master only]` or `[slave only]`). Categories with zero registered tests are hidden.

**Unknown category:**

```
[MyMod Test] Unknown category: foo_bar
```

**Ineligible category:**

```
[MyMod Test] Category mu_settings cannot run on this server type
```

---

## Dev Builds and Release Builds

### The `enable_dev_features` switch

Test code is not disabled at runtime; it is **not compiled** into release builds. The single switch is the Gradle property `enable_dev_features` in `gradle.properties`:

```properties
# Dev-only code (sandbox, unit tests, autotest). false = release: those packages are not compiled.
enable_dev_features = false
```

When it is not `true`, the root `build.gradle` excludes `net/kroia/modutilities/testing/**` and `net/kroia/modutilities/sandbox/**` from compilation in every module. Any release class that still references dev code fails to compile instead of silently shipping test code.

For a dev build, either set the property to `true` locally or pass it on the command line:

```
./gradlew build -Penable_dev_features=true
```

### How release code reaches dev code

`ModUtilitiesMod` has exactly one entry point into dev code, loaded via reflection so release code compiles without it:

```java
public static final boolean ENABLE_DEV_FEATURES = devClassesPresent(); // was DevBootstrap compiled in?

// in ModUtilitiesMod.init():
if (ENABLE_DEV_FEATURES) {
    // Class.forName("net.kroia.modutilities.testing.DevBootstrap").getMethod("init").invoke(null)
    initDevFeatures();
}
```

`DevBootstrap.init()` starts the sandbox (which registers the ModUtilities suites and the `/modutilities test` command) and the autotest hook. `ModUtilitiesMod.ENABLE_DEV_FEATURES` therefore always matches the Gradle switch; check it at runtime instead of a hand-maintained flag.

`TestRegistry.ENABLE_TESTS` still exists inside the `testing` package (always `true`); in a release build the whole package, including that flag, is absent.

### Dependent mods

The `testing` API only exists in a ModUtilities jar built with `enable_dev_features=true`. Dependent mods (BankSystem, StockMarket) use the same pattern for their own test code: exclude their test packages unless the switch is set, and reach them only through a reflective bootstrap. A dev build of a dependent mod needs a dev build of ModUtilities.

### Automated runs

In a dev build, `-Dmodutilities.autotest=<label>` (JVM argument of a client or server) runs every registered suite headless once the server is ready, writes a JSON report (`-Dmodutilities.autotest.results=<path>`, default `autotest-results.json`) and stops a dedicated server afterwards. Further `modutilities.autotest.*` properties (wait for a player, slaves or the master link, `mods` to run, `timeout`) are listed in the `AutoTest` class doc. `-Dmodutilities.tests.exclude` / `.include` / `.offline` select suites by `<modid>/<Category>/<Suite>` prefix keys, also for `gradlew :common:test` (see [Test selection](#test-selection)).

---

## Offline Tests (JUnit)

Suites that need no running game can also run as plain JUnit tests in `gradlew :common:test`, which is much faster.
Mark such a suite with `needsGame()` returning `false`, then add one `@TestFactory` to your mod's `common/src/test`:

```java
class MyModOfflineTests {
    @TestFactory
    Stream<DynamicTest> offline() {
        return OfflineTests.run(() -> MyModTestRegistration.register());
    }
}
```

`OfflineTests.run(registerSuites)` calls your registration (the same call your mod's init makes) and returns one JUnit test
per suite test, in the same order as in game:
- The tests are named `<modid>/<category>/<SuiteClass>/<test>`, plus `.../<setup>` and `.../<teardown>`.
- `teardown()` always runs. The tests of a suite are skipped when its `setup()` failed.
- `SKIP` results become JUnit skips; `FAIL` and `ERROR` become failures.
- `getServer()` returns `null`.

There is no game bootstrap and no mod loader. A suite that touches registries (items, `ItemStack`, registry codecs) calls
`SharedConstants.tryDetectVersion(); Bootstrap.bootStrap();` in its `setup()`; in game that call does nothing.
Code that reaches `@ExpectPlatform` methods (for example `ModChecker` or Architectury's `Platform`) or `UtilitiesPlatform`
fails offline, so such suites must keep `needsGame()` at `true`.

### Test selection

Three system properties select suites, in game (`-D` JVM argument) and in the offline run (`gradlew :common:test -D...`):

| Property | Effect |
|----------|--------|
| `modutilities.tests.include` | If set, only matching suites run; the rest count as excluded. |
| `modutilities.tests.exclude` | Matching suites do not run. Applied on top of `include`. |
| `modutilities.tests.offline` | Matching suites with `needsGame() == false` run in the offline run instead of in game. Unset: the offline run runs every capable suite, and the game runs every suite. |

Each value is a comma-separated list of keys `<modid>`, `<modid>/<Category>` or `<modid>/<Category>/<SuiteClass>`.
A key matches as a prefix by `/` segments, and a `*` segment matches any one segment (for example `mymod/*/MySuite`).
No regular expressions. In the offline run, an excluded suite shows up as one skipped `.../<excluded>` test.

---

## ModUtilities Internal Tests

MC_ModUtilities ships with its own set of test categories and suites to verify the library itself.

### Built-in Categories

Defined in `net.kroia.modutilities.testing.categories.ModUtilitiesTestCategories`:

| Category | ServerType | Context | Description |
|----------|------------|---------|-------------|
| `mu_networking` | `BOTH` | Yes | NetworkPacketManager and ARRS tests |
| `mu_streaming` | `BOTH` | Yes | StreamSystem tests |
| `mu_settings` | `MASTER_ONLY` | No | ModSettings save/load tests |
| `mu_persistence` | `MASTER_ONLY` | No | NBT/JSON persistence tests |
| `mu_events` | `BOTH` | No | DataEvent and Signal tests |
| `mu_multi_server` | `BOTH` | Yes | Master/slave communication tests |
| `mu_gui` | `BOTH` | No | GUI element logic tests |
| `mu_utilities` | `BOTH` | No | ColorUtilities, JsonUtilities, TimerMillis and ItemUtilities helper tests |
| `mu_luckperms` | `BOTH` | Yes | LuckPermsBridge with LuckPerms installed, and worlds after LuckPerms was removed |
| `mu_discord` | `BOTH` | No | DiscordWebhooks config, rendering, batching and rate limits (fake HTTP) |
| `mu_placeholder` | `BOTH` | Yes | PlaceholderRegistry resolved through Placeholder API (Fabric) |

### EventTests Coverage

The `EventTests` suite (`net.kroia.modutilities.testing.tests.EventTests`) provides 10 tests covering the `Signal` and `DataEvent` classes:

**Signal tests:**

| Test Name | What It Verifies |
|-----------|------------------|
| `signal_fires` | A single listener is called when the signal is notified. |
| `signal_multiple_listeners` | All attached listeners fire on a single notification. |
| `signal_remove_listener` | A removed listener is no longer called. |
| `signal_max_calls` | A listener with `maxCalls=2` stops firing after 2 notifications. |
| `signal_unlimited_calls` | A listener with `maxCalls=-1` fires indefinitely. |
| `signal_clear_listeners` | `removeListeners()` removes all listeners. |

**DataEvent tests:**

| Test Name | What It Verifies |
|-----------|------------------|
| `data_event_carries_data` | The data payload is delivered to the listener. |
| `data_event_multiple_listeners` | All listeners receive the same data payload. |
| `data_event_remove_listener` | A removed listener no longer receives events. |
| `data_event_max_calls` | A listener with `maxCalls=1` fires only once. |

These tests serve as both regression protection and a working reference for how to write test suites using the framework.
