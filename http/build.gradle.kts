plugins {
    maven
    `maven-publish`
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    compile("io.undertow:undertow-core:2.0.1.Final")
    compile(project(":eventsourced"))
    compile(project(":json"))
}




