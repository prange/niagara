import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

plugins {
    maven
    `maven-publish`
    java
    kotlin("jvm")
}

dependencies {
    compile(project(":niagara-data"))
    compile("io.vavr:vavr:0.10.0")
    compile("io.vavr:vavr-kotlin:0.10.0")
    compile(kotlin("stdlib-jdk8"))
}




repositories {
    mavenCentral()
}
val compileKotlin: KotlinCompile by tasks

compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks

compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}



