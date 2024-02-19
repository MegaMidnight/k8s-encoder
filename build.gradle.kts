plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.megamidnight"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("com.rabbitmq:amqp-client:5.20.0")
    implementation(platform("software.amazon.awssdk:bom:2.17.52"))
    implementation("software.amazon.awssdk:s3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.apache.logging.log4j:log4j-api:2.22.1")
    implementation("org.apache.logging.log4j:log4j-core:2.22.1")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.22.1")

}

tasks.test {
    useJUnitPlatform()
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
