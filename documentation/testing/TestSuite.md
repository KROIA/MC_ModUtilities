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
5. [Writing Tests](#writing-tests)
6. [Defining Categories](#defining-categories)
7. [Integrating in Your Mod](#integrating-in-your-mod)
8. [Commands](#commands)
9. [Production Builds](#production-builds)
10. [ModUtilities Internal Tests](#modutilities-internal-tests)

---

## Overview

The MC_ModUtilities Test Suite Framework provides **in-game unit testing** that runs via Minecraft chat commands. It is designed to:

- **Test mod logic inside a live Minecraft server** without external tooling.
- **Be shared across multiple mods** (ModUtilities, BankSystem, StockMarket, and any future mod) through a single framework living in the `net.kroia.modutilities.testing` package.
- **Filter tests by server role** in a master/slave multi-server setup, so master-only tests do not run on slave servers and vice versa.
- **Disable entirely in production** with a single boolean flag (`ENABLE_TESTS`), ensuring zero overhead in release builds.

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
        // Only registers if ENABLE_TESTS is true
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

#### Static Factory Methods

| Signature | Description |
|-----------|-------------|
| `TestResult.pass(String testName, @Nullable String message)` | Create a passing result. |
| `TestResult.fail(String testName, @Nullable String message)` | Create a failure with a message only. |
| `TestResult.fail(String testName, @Nullable String message, String expected, String actual)` | Create a failure with expected/actual values for diff display. |
| `TestResult.error(String testName, String message)` | Create an error result (uncaught exception, setup failure, etc.). |

#### Instance Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `getTestName()` | `String` | The name of the test. |
| `getStatus()` | `Status` | `PASS`, `FAIL`, or `ERROR`. |
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

#### Protected Methods

| Method | Description |
|--------|-------------|
| `addTest(String name, Supplier<TestResult> test)` | Register a named test. Call this inside `registerTests()`. |
| `@Nullable MinecraftServer getServer()` | Access the server instance (available after `setServer()` is called by the runner). |

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
public TestRunner(String modName, boolean isSlave, @Nullable MinecraftServer server)
```

| Parameter | Description |
|-----------|-------------|
| `modName` | Displayed in the chat prefix, e.g. `[MyMod Test]`. |
| `isSlave` | Whether the current server is a slave instance (used for category filtering). |
| `server`  | The `MinecraftServer` instance, passed to suites that need it. |

#### Public Methods

| Method | Description |
|--------|-------------|
| `runAll(ServerPlayer player)` | Run all registered test suites that are eligible for the current server type. Skips suites whose category `canRunOn(isSlave)` returns `false`. |
| `runCategory(ServerPlayer player, String categoryName)` | Run only suites matching the given category name. Reports an error if the category is unknown or not eligible. |
| `listCategories(ServerPlayer player)` | Print all known categories to chat, showing test counts. Ineligible categories are shown with strikethrough text and a reason. |

#### Chat Output Format

- **Per-test results** are indented with two spaces:
  - `  ✓ test_name` (green) for passing tests.
  - `  ✗ test_name` (red) for failures, followed by `Expected: X Got: Y` if available.
  - `  ! test_name` (yellow) for errors, followed by the error message.
- **Per-suite summaries** show `category_name: N/M passed, F failed, E errors`.
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
| `ENABLE_TESTS` | `public static final boolean` | Master switch. Set to `false` to disable the entire test framework. Default: `true`. |

#### Static Methods

| Method | Return Type | Description |
|--------|-------------|-------------|
| `register(TestSuite suite)` | `void` | Add a suite to the registry. No-op if `ENABLE_TESTS` is `false`. |
| `getTestSuites()` | `List<TestSuite>` | Unmodifiable list of all registered suites. |
| `getAvailableCategories(boolean isSlave)` | `List<String>` | Distinct category names whose `canRunOn(isSlave)` returns `true`. Used for command tab-completion. |
| `clear()` | `void` | Remove all registered suites. Useful for test reloading scenarios. |

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

## Production Builds

### The ENABLE_TESTS Kill Switch

In `TestRegistry`:

```java
public static final boolean ENABLE_TESTS = true;
```

Set this to `false` before building a production/release JAR. Because it is a `static final boolean`, the JVM/compiler can eliminate dead code paths at compile time.

### What Gets Disabled

When `ENABLE_TESTS = false`:

| Component | Behavior |
|-----------|----------|
| `TestRegistry.register()` | Returns immediately without storing the suite. |
| `TestCommandRegistration.register()` | Returns immediately; no commands are registered in Brigadier. |
| Test suite classes | Still exist in the JAR but are never instantiated or executed. |
| Category objects | Still constructed (static fields), but since no suites reference them, they have no effect. |

In practice, with `ENABLE_TESTS = false`, the test commands simply do not exist in the game, and no test code is ever invoked.

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
