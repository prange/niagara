import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar

plugins {
    maven
    `maven-publish`
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    val arrowversion = "0.7.2"
    compile("io.arrow-kt:arrow-core:$arrowversion")
    compile("io.arrow-kt:arrow-syntax:$arrowversion")
    compile("io.arrow-kt:arrow-typeclasses:$arrowversion")
    compile("io.arrow-kt:arrow-data:$arrowversion")
    compile("io.arrow-kt:arrow-generic:$arrowversion")
    compile("io.vavr:vavr:0.9.2")
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

publishing {
    repositories {
        mavenLocal()
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}

