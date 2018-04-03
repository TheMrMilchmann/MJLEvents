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
    maven
    signing
    id("com.zyxist.chainsaw") version "0.3.1"
}

val artifactName = "mjl-events"
val nextVersion = "1.1.0"

group = "com.github.themrmilchmann.mjl"
version = when (deployment.type) {
    BuildType.SNAPSHOT -> "$nextVersion-SNAPSHOT"
    else -> nextVersion
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

javaModule {
    exportedTestPackages = listOf("com.github.themrmilchmann.mjl.events.test")
}

artifacts {
    fun artifactNotation(artifact: String, classifier: String? = null) =
        if (classifier == null) {
            mapOf(
                "file" to File(buildDir, "libs/$artifact-$version.jar"),
                "name" to artifact,
                "type" to "jar"
            )
        } else {
            mapOf(
                "file" to File(buildDir, "libs/$artifact-$version-$classifier.jar"),
                "name" to artifact,
                "type" to "jar",
                "classifier" to classifier
            )
        }

    add("archives", artifactNotation(artifactName))
    add("archives", artifactNotation(artifactName, "sources"))
    add("archives", artifactNotation(artifactName, "javadoc"))
}

signing {
    isRequired = deployment.type == BuildType.RELEASE
    sign(configurations["archives"])
}

tasks {
    "test"(Test::class) {
        useTestNG()
    }

    "jar"(Jar::class) {
        baseName = artifactName

        manifest {
            attributes(mapOf(
                "Name" to project.name,
                "Specification-Version" to project.version,
                "Specification-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Leon Linhart <themrmilchmann@gmail.com>"
            ))
        }
    }

    val sourcesJar = "sourcesJar"(Jar::class) {
        baseName = artifactName
        classifier = "sources"
        from(java.sourceSets["main"].allSource)
    }

    val javadoc = "javadoc"(Javadoc::class)

    val javadocJar = "javadocJar"(Jar::class) {
        dependsOn(javadoc)

        baseName = artifactName
        classifier = "javadoc"
        from(javadoc.outputs)
    }

    val signArchives = "signArchives" {
        dependsOn(sourcesJar, javadocJar)
    }

    "uploadArchives"(Upload::class) {
        dependsOn(signArchives)

        repositories {
            withConvention(MavenRepositoryHandlerConvention::class) {
                mavenDeployer {
                    withGroovyBuilder {
                        "repository"("url" to deployment.repo) {
                            "authentication"(
                                "userName" to deployment.user,
                                "password" to deployment.password
                            )
                        }
                    }

                    if (deployment.type === BuildType.RELEASE) beforeDeployment { signing.signPom(this) }

                    pom.project {
                        withGroovyBuilder {
                            "artifactId"(artifactName)

                            "name"(project.name)
                            "description"("A minimal Java library which provides an efficient and modular EventBus solution for Java 9 and above.")
                            "packaging"("jar")
                            "url"("https://github.com/TheMrMilchmann/MJLEvents")

                            "licenses" {
                                "license" {
                                    "name"("The Apache License, Version 2.0")
                                    "url"("https://github.com/TheMrMilchmann/MJLEvents/blob/master/LICENSE")
                                    "distribution"("repo")
                                }
                            }

                            "developers" {
                                "developer" {
                                    "id"("TheMrMilchmann")
                                    "name"("Leon Linhart")
                                    "email"("themrmilchmann@gmail.com")
                                    "url"("https://github.com/TheMrMilchmann")
                                }
                            }

                            "scm" {
                                "connection"("scm:git:git://github.com/TheMrMilchmann/MJLEvents.git")
                                "developerConnection"("scm:git:git://github.com/TheMrMilchmann/MJLEvents.git")
                                "url"("https://github.com/TheMrMilchmann/MJLEvents.git")
                            }
                        }
                    }
                }
            }
        }
    }
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
    mavenCentral()
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testCompile("org.testng:testng:6.13.1")
    testCompile("com.google.code.findbugs:jsr305:3.0.2") // Required because of a restriction in the chainsaw plugin
}