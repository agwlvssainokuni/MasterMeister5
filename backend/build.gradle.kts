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

val npmBuildFrontend =
    tasks.register<com.github.gradle.node.npm.task.NpmTask>("npmBuildFrontend") {
        dependsOn(tasks.named("npmInstall"))
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
