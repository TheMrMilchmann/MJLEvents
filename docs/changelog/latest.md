### 3.0.0

_Released 2022 Mar 26_

#### Improvements

- EventBus instances are now self-cleaning by default since this is usually the
  desired behavior and less error-prone.
- The documentation has been rewritten to simplify and unify terminology.
- Handlers may now be registered explicitly via `EventHandler`.
- Improved documentation about the lifecycle of subscribers and GC eligibility.

#### Fixes

- Removed outdated documentation sections about dead events.

#### Breaking Changes

- `EventBus.build()` and `EventBus.build(Class)` signatures changed.
- EventBus member signatures and names changed.
  - `clear()` -> `cleanup()`
  - `purge()` -> `reset()`
  - `register()` -> `subscribe()`
  - `unregister()` -> `SubscriberHandle#unsubscribe()`
- Subscribers are no longer strongly referenced from a bus (use
  SubscriberHandles instead).
- EventBus instances are now self-cleaning by default.
- Removed the `Event` interface.
- Removed the `DeadEvent` class.