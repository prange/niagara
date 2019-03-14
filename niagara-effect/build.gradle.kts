import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    maven
    `maven-publish`
}


dependencies {
    compile(project(":niagara-data"))
    compile(kotlin("stdlib", "1.2.51"))
    compile(project(":niagara-data"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    testCompile("ch.qos.logback:logback-classic:1.0.11")
    testCompile("org.slf4j:slf4j-api:1.7.5")
    testCompile("junit:junit:4.12")
    compile("io.vavr:vavr:0.10.0")
    compile("io.vavr:vavr-kotlin:0.10.0")
    compile("org.jctools:jctools-core:2.1.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}



