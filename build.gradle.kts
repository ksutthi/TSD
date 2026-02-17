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

val jooqVersion = "3.19.18"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

sourceSets {
    main {
        java.srcDir("build/generated/jooq")
        kotlin.srcDir("build/generated/jooq")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("com.microsoft.sqlserver:mssql-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-jooq")

    jooqGenerator("com.h2database:h2")
    jooqGenerator("org.jooq:jooq-meta-extensions:$jooqVersion")

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
    version.set(jooqVersion)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.h2.Driver"

                    // 🟢 FINAL WINDOWS FIX:
                    // 1. Get absolute path (C:\Users\...)
                    // 2. FORCE replace backslashes with forward slashes (C:/Users/...)
                    // 3. Do NOT use .toURI() or 'file:' prefix.
                    val dbFile = project.file("src/main/resources/db/migration/V1__Init_Schema.sql")
                    val safePath = dbFile.absolutePath.replace("\\", "/")

                    println("DEBUG: Sending this path to H2: $safePath")

                    url = "jdbc:h2:mem:jooq-gen;MODE=MSSQLServer;INIT=RUNSCRIPT FROM '$safePath'"
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
                        packageName = "com.tsd.adapter.out.persistence.jooq.schema"
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