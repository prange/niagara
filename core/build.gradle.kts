plugins {
    kotlin("jvm")
    maven
    `maven-publish`
}


dependencies {
    val arrowversion = "0.7.2"
    compile(kotlin("stdlib", "1.2.51"))
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compile("io.arrow-kt:arrow-core:$arrowversion")
    compile("io.arrow-kt:arrow-syntax:$arrowversion")
    compile("io.arrow-kt:arrow-typeclasses:$arrowversion")
    compile("io.arrow-kt:arrow-data:$arrowversion")
    compile("io.arrow-kt:arrow-instances-core:$arrowversion")
    compile("io.arrow-kt:arrow-instances-data:$arrowversion")
    kapt("io.arrow-kt:arrow-annotations-processor:$arrowversion")
    compile("io.arrow-kt:arrow-optics:$arrowversion")
    compile("io.arrow-kt:arrow-generic:$arrowversion")
    testCompile("ch.qos.logback:logback-classic:1.0.11")
    testCompile("org.slf4j:slf4j-api:1.7.5")
    testCompile("junit:junit:4.12")
    compile("org.jctools:jctools-core:2.1.2")
    compile("org.functionaljava:functionaljava:4.7")
}



