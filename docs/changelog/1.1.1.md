### 1.1.1

_Released 2018 Apr 25_

#### Fixes

- Fixed an issue that could cause DeadEvents being dispatched to subscribers that expect a basic `Event` in a few edge-
  cases.
- Improved the general performance of `EventBus#post(Event)` by handling dead-events separately.