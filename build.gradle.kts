// ./gradlew shadowJar
//
//./gradlew jar
// ./build/libs$ jlink   --add-modules java.base,java.desktop,java.prefs,spacemonger   --output spacemonger1-runtime   --compress 10   --strip-debug   --no-header-files   --no-man-pages --module-path .
// ./spacemonger1-runtime/bin/java -m spacemonger/spacemonger1.App
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    id("com.gradleup.shadow") version "9.2.2"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
//    implementation("com.formdev:flatlaf:3.6.2")
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

application {
    // Define the main class for the application.
    mainClass.set("spacemonger1.App")
}

tasks.test {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "spacemonger1.App"
}
