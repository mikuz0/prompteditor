plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
}

group = "com.pandakind"
version = "2.2.0"

repositories {
    mavenCentral()
}

val junitVersion = "5.10.2"

kotlin {
    jvmToolchain(21)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set("com.pandakind.prompteditor.PromptEditorAppKt")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    implementation("org.fxmisc.richtext:richtextfx:0.11.3")

    implementation("io.github.geniot:jortho:1.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}