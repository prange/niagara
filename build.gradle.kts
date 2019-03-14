plugins {
    maven
    groovy
    java
    kotlin("jvm") version "1.3.21"
}


var kantGroup = "org.kantega.niagara"
var kantVersion = "1.0-SNAPSHOT"

description = """"""




allprojects {

    group = kantGroup
    version = kantVersion

    repositories {
        mavenCentral()
        mavenLocal()
    }



}






