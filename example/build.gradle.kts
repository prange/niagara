plugins {
    maven
    java
    kotlin("jvm")
    id("org.jetbrains.dokka")
}

dependencies {
    compile(project(":http"))
}




