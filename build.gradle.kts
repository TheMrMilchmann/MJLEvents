/*
 * Copyright 2018 Leon Linhart
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
plugins {
    java
    signing
    `maven-publish`
}

val artifactName = "mjl-events"
val nextVersion = "1.1.4"

group = "com.github.themrmilchmann.mjl"
version = when (deployment.type) {
    BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    val compileJava: JavaCompile by getting

    create<JavaCompile>("compileJava9") {
        val jdk9Props = arrayOf(
            "JDK9_HOME",
            "JAVA9_HOME",
            "JDK_19",
            "JDK_9"
        )

        val ftSource = fileTree("src/main-jdk9/java")
        ftSource.include("**/*.java")
        options.sourcepath = files("src/main-jdk9/java")
        source = ftSource

        classpath = files()
        destinationDir = File(buildDir, "classes/java-9/main")

        sourceCompatibility = "9"
        targetCompatibility = "9"

        afterEvaluate {
            // module-path hack
            options.compilerArgs.add("--module-path")
            options.compilerArgs.add(compileJava.classpath.asPath)
        }

        val jdk9Home = jdk9Props.map { System.getenv(it)?.let { File(it) } }
            .filterNotNull()
            .firstOrNull(File::exists) ?: throw Error("Could not find valid JDK9 home")
        options.forkOptions.javaHome = jdk9Home
        options.isFork = true
    }

    "test"(Test::class) {
        useTestNG()
    }

    "jar"(Jar::class) {
        dependsOn("compileJava9")

        baseName = artifactName

        into("META-INF/versions/9") {
            from(tasks["compileJava9"].outputs.files) {
                include("module-info.class")
            }
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
        baseName = artifactName
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }

    val javadoc = "javadoc"(Javadoc::class)

    create<Jar>("javadocJar") {
        dependsOn(javadoc)

        baseName = artifactName
        classifier = "javadoc"
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
    (publications) {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])

            artifactId = artifactName

            pom {
                name.set(project.name)
                description.set("A minimal Java library which provides an efficient and modular EventBus solution.")
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
    onlyIf { deployment.type === BuildType.RELEASE }
}

val Project.deployment: Deployment
    get() = when {
        hasProperty("release") -> Deployment(
            BuildType.RELEASE,
            "https://oss.sonatype.org/service/local/staging/deploy/maven2/",
            getProperty("sonatypeUsername"),
            getProperty("sonatypePassword")
        )
        hasProperty("snapshot") -> Deployment(
            BuildType.SNAPSHOT,
            "https://oss.sonatype.org/content/repositories/snapshots/",
            getProperty("sonatypeUsername"),
            getProperty("sonatypePassword")
        )
        else -> Deployment(BuildType.LOCAL, repositories.mavenLocal().url.toString())
    }

fun Project.getProperty(k: String) =
    if (extra.has(k))
        extra[k] as String
    else
        System.getenv(k)

enum class BuildType {
    LOCAL,
    SNAPSHOT,
    RELEASE
}

data class Deployment(
    val type: BuildType,
    val repo: String,
    val user: String? = null,
    val password: String? = null
)

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testCompile("org.testng:testng:6.14.3")
}