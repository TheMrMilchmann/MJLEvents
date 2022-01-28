/*
 * Copyright 2018-2021 Leon Linhart
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
@file:Suppress("UnstableApiUsage")

import com.github.themrmilchmann.build.*
import com.github.themrmilchmann.build.BuildType

plugins {
    `java-library`
    signing
    `maven-publish`
}

val artifactName = "mjl-events"
val nextVersion = "3.0.0"

group = "com.github.themrmilchmann.mjl"
version = when (deployment.type) {
    BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    compileJava {
        /* Java 8 is the minimum supported version. */
        options.release.set(8)
    }

    compileTestJava {
        /* Java 8 is used for testing. */
        options.release.set(8)
    }

    /*
     * To make the library a fully functional module for Java 9 and later, we make use of multi-release JARs. To be
     * precise: The module descriptor (module-info.class) is placed in /META-INF/versions/9 to be available on
     * Java 9 and later only.
     *
     * (Additional Java 9 specific functionality may also be used and is handled by this task.)
     */
    val compileJava9 = create<JavaCompile>("compileJava9") {
        destinationDirectory.set(File(buildDir, "classes/java-jdk9/main"))

        // Remove when https://github.com/gradle/gradle/issues/18262 is fixed
        modularity.inferModulePath.set(false)

        val java9Source = fileTree("src/main/java-jdk9") {
            include("**/*.java")
        }

        source = java9Source
        options.sourcepath = files(sourceSets["main"].java.srcDirs) + files(java9Source.dir)

        classpath = files()

        options.release.set(9)

        afterEvaluate {
            options.compilerArgs.add("--module-path")
            options.compilerArgs.add(compileJava.get().classpath.asPath)
            options.compilerArgs.add("--module-version")
            options.compilerArgs.add("$version")
        }
    }

    classes {
        dependsOn(compileJava9)
    }

    jar {
        archiveBaseName.set(artifactName)

        into("META-INF/versions/9") {
            from(compileJava9.outputs.files.filter(File::isDirectory))
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
            from(compileJava9.inputs.files.filter(File::isDirectory))
            includeEmptyDirs = false
        }
    }

    javadoc {
        with (options as StandardJavadocDocletOptions) {
            tags = listOf(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )

            addStringOption("-release", "8")
        }
    }

    create<Jar>("javadocJar") {
        dependsOn(javadoc)

        archiveBaseName.set(artifactName)
        archiveClassifier.set("javadoc")
        from(javadoc.get().outputs)
    }

    test {
        useJUnitPlatform()
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
                description.set("A minimal Java library which provides an efficient and modular in-process event-system.")
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
    isRequired = (deployment.type === BuildType.RELEASE)
    sign(publishing.publications)
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnlyApi(group = "com.google.code.findbugs", name = "jsr305", version = "3.0.2")

    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter", version = "5.7.1")
}