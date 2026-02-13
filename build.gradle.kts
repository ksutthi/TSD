import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("nu.studer.jooq") version "9.0"
}

group = "com.tsd"
version = "0.0.1-SNAPSHOT"

// 🟢 DEFINE THE VERSION ONCE (Matches what Spring Boot 3.4.2 uses)
val jooqVersion = "3.19.18"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- 1. SPRING BOOT CORE ---
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- 2. KOTLIN & COROUTINES ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    // --- 3. DATABASE (Runtime) ---
    implementation("com.microsoft.sqlserver:mssql-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq") // Uses Spring's version (3.19.18)

    // --- 4. JOOQ GENERATOR (Build Time) ---
    // 🟢 FORCE SAME VERSION HERE
    jooqGenerator("com.h2database:h2")
    jooqGenerator("org.jooq:jooq-meta-extensions:$jooqVersion")

    // --- 5. TESTING ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<ProcessResources>("processResources") {
    outputs.upToDateWhen { false }
}

// ============================================================================
// JOOQ GENERATOR CONFIGURATION
// ============================================================================
jooq {
    version.set(jooqVersion) // 🟢 FORCE SAME VERSION HERE

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"
                    url = "jdbc:h2:mem:jooq-gen;INIT=RUNSCRIPT FROM 'src/main/resources/schema.sql'"
                    user = "sa"
                    password = ""
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.h2.H2Database"
                        inputSchema = "PUBLIC"
                        includes = ".*"
                        excludes = ""
                    }
                    target.apply {
                        packageName = "com.tsd.platform.persistence.jooq.schema"
                        directory = "build/generated/jooq"
                    }
                }
            }
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn(tasks.named("generateJooq"))
}