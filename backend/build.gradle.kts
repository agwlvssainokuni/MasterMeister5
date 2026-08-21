/*
 * Copyright 2026 agwlvssainokuni
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
    war
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.0.2"
}

group = "cherry"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    // JWT issuing/verification (Unit 2, nfr-design-plan.md Question 4 = HS256).
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    // H2 support ships inside flyway-core itself — no separate flyway-database-h2
    // artifact exists (unlike flyway-mysql / flyway-database-postgresql etc.).
    implementation("org.flywaydb:flyway-core")
    implementation("com.h2database:h2")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    implementation("com.bucket4j:bucket4j-core:8.10.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation(project(":libs:java-mustache-processor:cherry-mustache-core"))
    // spring-boot-starter-tomcat (the coarser starter) pulled spring-web out of
    // the runtime classpath entirely when marked providedRuntime; the finer
    // -runtime artifact avoids that.
    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // Spring Boot 4.x split @WebMvcTest / @DataJpaTest out of
    // spring-boot-starter-test into per-slice starters.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.security:spring-security-test")
    // @DataJpaTest does not import Flyway autoconfiguration by default; tests
    // that need it (AppThemeRepositoryImplTest) add it back explicitly via
    // @ImportAutoConfiguration, which requires this class on the classpath.
    testImplementation("org.springframework.boot:spring-boot-flyway")
    testImplementation("net.jqwik:jqwik:1.9.1")
}

tasks.withType<Test> {
    useJUnitPlatform {
        includeEngines("junit-jupiter", "jqwik")
    }
}

node {
    version = "24.0.0"
    download = true
    nodeProjectDir = file("${rootDir}/frontend")
}

// frontend's `make-you-chic-ui` dependency is a bare file: reference to the
// submodule (README.md setup step 2) — npm's file: linking does not build
// the target package itself, so without this, npmBuildFrontend fails to
// resolve "make-you-chic-ui" the first time (confirmed by deleting
// libs/make-you-chic-ui/packages/make-you-chic-ui/dist and re-running
// bootWar). Building it here makes a single `bootWar` self-contained.
val makeYouChicUiDir = file("${rootDir}/libs/make-you-chic-ui")

val npmInstallMakeYouChicUi =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmInstallMakeYouChicUi") {
        workingDir = makeYouChicUiDir
        npmCommand = listOf("install")
        inputs.file(makeYouChicUiDir.resolve("package.json"))
        inputs.file(makeYouChicUiDir.resolve("package-lock.json"))
        outputs.dir(makeYouChicUiDir.resolve("node_modules"))
    }

val npmBuildMakeYouChicUi =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuildMakeYouChicUi") {
        dependsOn(npmInstallMakeYouChicUi)
        workingDir = makeYouChicUiDir
        npmCommand = listOf("run", "build", "-w", "make-you-chic-ui")
        outputs.dir(makeYouChicUiDir.resolve("packages/make-you-chic-ui/dist"))
    }

val npmBuildFrontend =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuildFrontend") {
        dependsOn(tasks.named("npmInstall"), npmBuildMakeYouChicUi)
        npmCommand = listOf("run", "build")
        // vite.config.ts outputs directly into src/main/resources/static (requirements.md
        // §3: single WAR build). Declared as outputs so Gradle can skip this task when
        // nothing changed.
        outputs.dir(layout.projectDirectory.dir("src/main/resources/static"))
    }

tasks.named("processResources") {
    mustRunAfter(npmBuildFrontend)
}

tasks.named("war") {
    dependsOn(npmBuildFrontend)
}

tasks.named("bootWar") {
    dependsOn(npmBuildFrontend)
}
