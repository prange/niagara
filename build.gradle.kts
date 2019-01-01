plugins {
    base
    idea
    maven
    `maven-publish`
    `build-scan`
    java
    kotlin("jvm") version "1.2.60"
    id("org.jetbrains.dokka") version "0.9.16"
    id("org.jetbrains.kotlin.kapt") version "1.2.60"
}


var kantGroup = "org.kantega.niagara"
var kantVersion = "1.0-SNAPSHOT"

description = """"""





buildScan {
    setTermsOfServiceUrl("https://gradle.com/terms-of-service")
    setTermsOfServiceAgree("yes")
}

allprojects {

    group = kantGroup
    version = kantVersion

    repositories {
        mavenCentral()
        mavenLocal()
    }



}






