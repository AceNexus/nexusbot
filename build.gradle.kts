plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.acenexus.tata"

// 讀取 Git Tag 作為 version，沒有 Tag 時預設為 0.0.1-SNAPSHOT
val gitVersion: String by lazy {
    try {
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0").start()
        process.inputStream.bufferedReader().readText().trim()
    } catch (e: Exception) {
        "0.0.1-SNAPSHOT"
    }
}

version = gitVersion

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2024.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-bootstrap")

    // DataBase
    implementation("com.mysql:mysql-connector-j:8.3.0")
    runtimeOnly("com.h2database:h2")

    // LINE Bot SDK
    implementation("com.linecorp.bot:line-bot-messaging-api-client:9.8.0")
    implementation("com.linecorp.bot:line-bot-webhook:9.8.0")
    implementation("com.linecorp.bot:line-bot-spring-boot-webmvc:9.8.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// 禁用標準 jar 任務
tasks.jar {
    enabled = false
}

// 保持 bootJar 任務可用
tasks.bootJar {
    enabled = true
}