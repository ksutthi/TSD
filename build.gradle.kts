plugins {
    // 🟢 STABLE KOTLIN (Works perfectly with Gradle 8.x)
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"

    // 🟢 STABLE SPRING BOOT (Latest solid release)
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.tsd"
version = "0.0.1-SNAPSHOT"
description = "TSD"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // --- 1. CORE WEB & DATA ---
    implementation("org.springframework.boot:spring-boot-starter-web") // Includes MVC & Tomcat
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    // 🟢 SQL Server
    implementation("com.microsoft.sqlserver:mssql-jdbc")

    // --- 2. BATCH (The critical part) ---
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // --- 3. SECURITY ---
    implementation("org.springframework.boot:spring-boot-starter-security")

    // --- 4. KOTLIN ---
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // 🟢 ADD THIS LINE TO FIX 'runBlocking', 'async', 'delay':
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // --- 5. TEST ---
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.named<ProcessResources>("processResources") {
    // ☠️ FORCE update: Always treat resources as "out of date"
    outputs.upToDateWhen { false }
}

tasks.withType<JavaExec> {
    // Force the app to run with UTF-8 encoding
    systemProperty("file.encoding", "UTF-8")
    // Also force the console to output standard streams in UTF-8
    standardOutput = System.out
    errorOutput = System.err
    jvmArgs("-Dfile.encoding=UTF-8")
}