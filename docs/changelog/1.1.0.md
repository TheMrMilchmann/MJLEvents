### 1.1.0

_Released 2018 Apr 20_


#### Improvements

- Added the ability to customize the handling of errors that occur while an event is dispatched to a subscriber via
  `DispatchErrorHandler(s)`.
- Added public getters that expose the bus and the origin of a `EventBus.Subscriber`.


#### Fixes

- Changed visibility of `EventDispatcher#dispatch` to `protected` to enable overriding the method.