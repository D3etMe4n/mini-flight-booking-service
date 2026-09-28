plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("jacoco")
}

group = "com.cnpm"
version = "0.0.1-SNAPSHOT"
description = "mini-flight-booking-service"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

extra["springModulithVersion"] = "2.1.1"

dependencies {
                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-starter-webmvc")
                implementation("org.springframework.boot:spring-boot-starter-data-jpa")
                implementation("org.springframework.boot:spring-boot-starter-data-redis")
                implementation("org.springframework.boot:spring-boot-starter-validation")
                implementation("org.springframework.modulith:spring-modulith-starter-core")
                implementation("org.springframework.modulith:spring-modulith-starter-jpa")
                implementation("com.fasterxml.jackson.core:jackson-databind")
                compileOnly("org.projectlombok:lombok")
                runtimeOnly("org.postgresql:postgresql")
                annotationProcessor("org.projectlombok:lombok")
                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
                testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
                testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
                testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
                testImplementation("org.springframework.boot:spring-boot-testcontainers")
                testImplementation("org.springframework.modulith:spring-modulith-starter-test")
                testImplementation("org.testcontainers:testcontainers-junit-jupiter")
                testImplementation("org.testcontainers:testcontainers-postgresql")
                testImplementation("org.awaitility:awaitility:4.2.2")
                testCompileOnly("org.projectlombok:lombok")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
                testAnnotationProcessor("org.projectlombok:lombok")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.modulith:spring-modulith-bom:${property("springModulithVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

//tasks.jacocoTestReport {
//    dependsOn(tasks.test)
//    reports {
//        xml.required.set(true)
//        html.required.set(true)
//    }
//}
