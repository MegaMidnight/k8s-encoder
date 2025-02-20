plugins {
    kotlin("jvm") version "1.9.22"
    jacoco
}

group = "com.megamidnight"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Main dependencies
    implementation("com.rabbitmq:amqp-client:5.25.0")
    implementation(platform("software.amazon.awssdk:bom:2.30.24"))
    implementation("software.amazon.awssdk:s3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.24.3")
    implementation("redis.clients:jedis:5.2.0")

    // Test dependencies
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.testcontainers:testcontainers:1.20.5")
    testImplementation("org.testcontainers:junit-jupiter:1.20.5")
    // https://mvnrepository.com/artifact/org.wiremock/wiremock
    testImplementation("org.wiremock:wiremock:3.12.0")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
    }
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.register<Copy>("copyClasses") {
    from("build/classes/kotlin/main")
    into("/app")
}
tasks.register<JavaExec>("runApp") {
    classpath = files("app")
    mainClass.set("MainKt")
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(*configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }.toTypedArray())
    archiveBaseName.set("kotlin-ffmpeg-x265")
    archiveVersion.set("1.0.0")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(21)
}
