import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

plugins {
    maven
    `maven-publish`
    java
    kotlin("jvm")
}

dependencies {
    compile(project(":niagara-http"))
    compile("io.undertow:undertow-core:2.0.1.Final")
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



