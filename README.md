# MJL Events

[![License](https://img.shields.io/badge/license-Apache%202.0-yellowgreen.svg?style=flat-square&label=License)](https://github.com/TheMrMilchmann/MJLEvents/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.themrmilchmann.mjl/mjl-events.svg?style=flat-square&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/com.github.themrmilchmann.mjl/mjl-events)
[![JavaDoc](https://img.shields.io/maven-central/v/com.github.themrmilchmann.mjl/mjl-events.svg?style=flat-square&label=JavaDoc&color=blue)](https://javadoc.io/doc/com.github.themrmilchmann.mjl/mjl-events)

A **m**inmal **J**ava **l**ibrary which provides an efficient and modular
in-process Event-System (for Java 8 and later).

The API of this library has been heavily influenced by [guava](https://github.com/google/guava)
and other implementations. It was designed to be fully compatible with Java 9
and the module-system.


## Getting Started

```java
public class Sample {

    public static void main(String[] args) {
        // Create a basic EventBus
        EventBus<Event> bus = EventBus.build();

        // Subscribe a handler
        SubscriberHandle handle = bus.subscribe(SimpleEvent.class, () -> System.our.println("Received Event"));

        // Post an event
        Event event = new Event();
        bus.post(event);

        // Unsubscribe a handler
        handle.unsubscribe();
    }

    public static class Event {}

}
```


## Building from source

### Setup

This project uses [Gradle's toolchain support](https://docs.gradle.org/7.3.3/userguide/toolchains.html)
to detect and select the JDKs required to run the build. Please refer to the
build scripts to find out which toolchains are requested.

An installed JDK 1.8 (or later) is required to use Gradle.

### Building

Once the setup is complete, invoke the respective Gradle tasks using the
following command on Unix/macOS:

    ./gradlew <tasks>

or the following command on Windows:

    gradlew <tasks>

Important Gradle tasks to remember are:
- `clean`                   - clean build results
- `build`                   - assemble and test the Java library
- `publishToMavenLocal`     - build and install all public artifacts to the
                              local maven repository

Additionally `tasks` may be used to print a list of all available tasks.


## License

Copyright 2018-2021 Leon Linhart

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.