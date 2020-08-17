# MJL Events

[![License](https://img.shields.io/badge/license-Apache%202.0-yellowgreen.svg?style=flat-square&label=License)](https://github.com/TheMrMilchmann/MJLEvents/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.themrmilchmann.mjl/mjl-events.svg?style=flat-square&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/com.github.themrmilchmann.mjl/mjl-events)
[![JavaDoc](https://img.shields.io/maven-central/v/com.github.themrmilchmann.mjl/mjl-events.svg?style=flat-square&label=JavaDoc&color=blue)](https://javadoc.io/doc/com.github.themrmilchmann.mjl/mjl-events)

A **m**inmal **J**ava **l**ibrary which provides an efficient and modular Event-System (for Java 8 and later).

The API of this library has been heavily influenced by [guava](https://github.com/google/guava) and other
implementations. It was designed to be fully compatible with Java 9 and the JPMS.


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


## Building from source

### Setup

A complete build expects multiple JDK installations set up as follows:
1. JDK 1.8 (used to compile the basic library)
2. JDK   9 (used to compile the module descriptor)
3. JDK  14 (used to generate the JavaDoc)

These JDKs must be made visible to the build process by setting up
environment variables (or [Gradle properties](https://docs.gradle.org/current/userguide/build_environment.html#sec:gradle_configuration_properties))
for each JDK version as follows:

```
JAVA_HOME="path to JDK 1.8"
JDK_8="path to JDK 1.8"
JDK_9="path to JDK 9"
JDK_14="path to JDK 14"
```

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