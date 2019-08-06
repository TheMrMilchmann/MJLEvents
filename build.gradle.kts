/*
 * Copyright 2018-2019 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.github.themrmilchmann.build.*
import org.gradle.internal.jvm.*

plugins {
    java
    id("me.champeau.gradle.jmh") version "0.4.7"
    signing
    `maven-publish`
}

val artifactName = "mjl-events"
val nextVersion = "2.1.0"

group = "com.github.themrmilchmann.mjl"
version = when (deployment.type) {
    com.github.themrmilchmann.build.BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
}

val currentJVMAtLeast9 = Jvm.current().javaVersion!! >= JavaVersion.VERSION_1_9

java {
    /*
     * Source- and target-compatibility are set here so that an IDE can easily pick them up. They are, however,
     * overwritten by the compileJava task (as part of a workaround).
     */
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileJava {
        /* JDK 8 does not support the --release option */
        if (Jvm.current().javaVersion!! > JavaVersion.VERSION_1_8) {
            // Workaround for https://github.com/gradle/gradle/issues/2510
            options.compilerArgs.addAll(listOf("--release", "8"))
        }
    }

    val compileJava9 = create<JavaCompile>("compileJava9") {
        val ftSource = fileTree("src/main/java-jdk9")
        ftSource.include("**/*.java")
        options.sourcepath = files("src/main/java-jdk9")
        source = ftSource

        classpath = files()
        destinationDir = File(buildDir, "classes/java-jdk9/main")

        sourceCompatibility = "9"
        targetCompatibility = "9"

        // Workaround for https://github.com/gradle/gradle/issues/2510
        options.compilerArgs.addAll(listOf("--release", "9"))

        afterEvaluate {
            // module-path hack
            options.compilerArgs.add("--module-path")
            options.compilerArgs.add(compileJava.get().classpath.asPath)
        }

        /*
         * If the JVM used to invoke Gradle is JDK 9 or later, there is no reason to require a separate JDK 9 instance.
         */
        if (!currentJVMAtLeast9) {
            val jdk9Props = arrayOf(
                "JDK9_HOME",
                "JAVA9_HOME",
                "JDK_19",
                "JDK_9"
            )

            val jdk9Home = jdk9Props.mapNotNull { System.getenv(it) }
                .map { File(it) }
                .firstOrNull(File::exists) ?: throw Error("Could not find valid JDK9 home")
            options.forkOptions.javaHome = jdk9Home
            options.isFork = true
        }
    }

    test {
        useTestNG()
    }

    jar {
        dependsOn(compileJava9)

        archiveBaseName.set(artifactName)

        into("META-INF/versions/9") {
            from(compileJava9.outputs.files.filter(File::isDirectory)) {
                exclude("**/Stub.class")
            }

            includeEmptyDirs = false
        }

        manifest {
            attributes(mapOf(
                "Name" to project.name,
                "Specification-Version" to project.version,
                "Specification-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>",
                "Multi-Release" to "true"
            ))
        }
    }

    create<Jar>("sourcesJar") {
        archiveBaseName.set(artifactName)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)

        into("META-INF/versions/9") {
            from(compileJava9.inputs.files.filter(File::isDirectory)) {
                exclude("**/Stub.java")
            }

            includeEmptyDirs = false
        }
    }

    create<Jar>("javadocJar") {
        dependsOn(javadoc)

        archiveBaseName.set(artifactName)
        archiveClassifier.set("javadoc")
        from(javadoc.get().outputs)
    }
}

publishing {
    repositories {
        maven {
            url = uri(deployment.repo)

            credentials {
                username = deployment.user
                password = deployment.password
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = artifactName

            pom {
                name.set(project.name)
                description.set("A minimal Java library which provides an efficient and modular Event-System.")
                packaging = "jar"
                url.set("https://github.com/TheMrMilchmann/MJLEvents")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://github.com/TheMrMilchmann/MJLEvents/blob/master/LICENSE")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("TheMrMilchmann")
                        name.set("Leon Linhart")
                        email.set("themrmilchmann@gmail.com")
                        url.set("https://github.com/TheMrMilchmann")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/TheMrMilchmann/MJLEvents.git")
                    developerConnection.set("scm:git:git://github.com/TheMrMilchmann/MJLEvents.git")
                    url.set("https://github.com/TheMrMilchmann/MJLEvents.git")
                }
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

val signMavenJavaPublication by tasks.getting {
    onlyIf { deployment.type === com.github.themrmilchmann.build.BuildType.RELEASE }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testCompile("org.testng:testng:6.14.3")
}