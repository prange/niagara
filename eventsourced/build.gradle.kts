plugins {
    maven
    `maven-publish`
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    compile("com.h2database:h2:1.4.197")
    compile(project(":core"))
}




