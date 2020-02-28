plugins {
    kotlin("jvm") version "1.3.61"
    maven
}

var pluginGroup = "com.github.patrick-mc"
var pluginVersion = "0.1-beta"
group = pluginGroup
version = pluginVersion

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://jitpack.io/")
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    implementation("com.github.noonmaru:tap:1.0.1")
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }
    javadoc {
        options.encoding = "UTF-8"
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}