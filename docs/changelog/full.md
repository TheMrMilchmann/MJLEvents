### 2.0.1

_Released 2019 Aug 06_

#### Improvements

- Added [Gradle metadata](https://blog.gradle.org/gradle-metadata-1.0) in published artifacts.
- Replaced all occurrences of `http` in links with `https`.
- Improved project documentation.
- Made build scripts more flexible.


---

### 2.0.0

_Released 2019 Mar 07_

#### Improvements

- Generified `EventBus` and related APIs.
- Improved JavaDoc for dispatchers.
- Include multi-release sources in source artifact.
- Changed handling of dead events to be callback based.

#### Fixes

- `EventBus.Builder#setDispatchErrorHandler` now accepts `null` values as intended

#### Breaking Changes

- Removed public `EventBus.Builder` constructor (use `EventBus#builder` instead).
- Removed default dispatch printing behavior.
- Refactored `DeadEvent` handling to be callback based.
    - `DeadEvent` no longer implements `Event` and may not be used as such.


---

### 1.2.0

_Released 2018 Oct 15_

#### Improvements

- Added a factory method to `EventBus` and deprecated the public constructor for `EventBus#Builder`.
- JavaDoc deployment to Github Pages.


---

### 1.1.3

_Released 2018 Jun 09_

#### Fixes

- Re-added the `module-info` file that was missing in `1.1.2` due to a bug in the build scripts.


---

### 1.1.2

_Released 2018 Jun 05_

#### Improvements

- Lowered the required Java version to Java 8 (from 9).


---

### 1.1.1

_Released 2018 Apr 25_

#### Fixes

- Fixed an issue that could cause DeadEvents being dispatched to subscribers that expect a basic `Event` in a few edge-
  cases.
- Improved the general performance of `EventBus#post(Event)` by handling dead-events separately.


---

### 1.1.0

_Released 2018 Apr 20_


#### Improvements

- Added the ability to customize the handling of errors that occur while an event is dispatched to a subscriber via
  `DispatchErrorHandler(s)`.
- Added public getters that expose the bus and the origin of a `EventBus.Subscriber`.

#### Fixes

- Changed visibility of `EventDispatcher#dispatch` to `protected` to enable overriding the method.


---

### 1.0.0

_Released 2018 Mar 31_

The first official release.