# MJL Events

[![License](https://img.shields.io/badge/license-Apache%202.0-yellowgreen.svg?style=flat-square)](https://github.com/TheMrMilchmann/MJLEvents/blob/master/LICENSE)
[![Build Status](https://img.shields.io/travis/TheMrMilchmann/MJLEvents/master.svg?style=flat-square)](https://travis-ci.org/TheMrMilchmann/MJLEvents)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.themrmilchmann.mjl/mjl-events.svg?style=flat-square&label=maven%20central)](https://maven-badges.herokuapp.com/maven-central/com.github.themrmilchmann.mjl/mjl-events)

A **m**inmal **J**ava **l**ibrary which provides an efficient and modular Event-System (for Java 8 and later).

The API of this library has been heavily influenced by [guava](https://github.com/google/guava) and other
implementations. It was designed to be fully compatible with Java 9 and the JPMS.

[JavaDoc for MJL Events.](https://themrmilchmann.github.io/MJLEvents/)


## Getting Started

```java
public class Sample {
    
    // Creating a basic EventBus
    private static final EventBus<Event> bus = EventBus.builder().build();
    
    static {
        // Registering a subscriber
        bus.register(Sample.class, MethodHandles.lookup());
        
        // Posting an event
        Event event = new SimpleEvent();
        bus.post(event);
    }
    
    @EventSubscriber
    public static void handleEvent(Event event) {
        // Handle the event
    } 
    
    public static class SimpleEvent implements Event {}

}
```


## Installing

To built MJLEvents the JDK used to invoke Gradle (usually the JDK in `JAVA_HOME`) must either be a version of JDK 8, or
support targeting Java 8 (via `--release 8` option). If the former is the case another JDK supporting targeting Java 9
(via `--release 9` option) must be available with one of the following environment variables pointing at it:
`JDK9_HOME`, `JAVA9_HOME`, `JDK_19`, `JDK_9`.

Starting the actual build is simply a matter of invoking Gradle. For example: In order to run a full build of the
project (including tests), call

    ./gradlew build

To reproduce snapshot or release builds, passing the `-Psnapshot` or `-Prelease` parameter respectively is required.
When the `-Prelease` option is used, binaries will be signed, thus the `signing` plugin needs to be configured.


## License

Copyright 2018-2019 Leon Linhart

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.