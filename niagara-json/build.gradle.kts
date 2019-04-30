import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

plugins {
    kotlin("jvm")
    maven
    `maven-publish`
}

dependencies {
    compile(project(":niagara-data"))
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
    from(sourceSets["main"].allSource)
}



