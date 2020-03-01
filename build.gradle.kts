plugins {
    kotlin("jvm") version "1.3.61"
    id("org.jetbrains.dokka") version "0.10.0"
    `maven-publish`
}

group = properties["projectGroup"]!!
version = properties["projectVersion"]!!

repositories {
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
    maven(url = "https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven(url = "https://jitpack.io/")
    maven(url = "https://dl.bintray.com/kotlin/dokka")
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

    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    val dokka by getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/dokka"

        configuration {
            includeNonPublic = true
            jdkVersion = 8
        }
    }
    
    create<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        from(dokka)
        dependsOn(dokka)
    }

    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    create<Copy>("distJar") {
        from(jar)
        into("W:\\Servers\\1.12\\plugins")
    }
}

publishing {
    publications {
        create<MavenPublication>("sword") {
            from(components["java"])

            artifact(tasks["sourcesJar"])
            artifact(tasks["dokkaJar"])

            pom {
                name.set("Sword")
                description.set("A Sword Plugin written in Kotlin using Tap Library")
                url.set("https://github.com/patrick-mc/sword")
                licenses {
                    license {
                        name.set("GNU Affero General Public License")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.en.html")
                    }
                }
                developers {
                    developer {
                        id.set("patrick-mc")
                        name.set("PatrickKR")
                        email.set("mailpatrickkorea@gmail.com")
                        url.set("https://github.com/patrick-mc")
                        roles.addAll("manager", "developer")
                        timezone.set("Asia/Seoul")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/patrick-mc/sword.git")
                    developerConnection.set("scm:git:ssh://github.com/patrick-mc/sword.git")
                    url.set("https://github.com/patrick-mc/sword")
                }
            }
        }
    }
}
