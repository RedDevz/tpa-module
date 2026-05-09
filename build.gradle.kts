plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
}

group = "de.reddev"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${project.property("paperVersion")}")
    compileOnly(files("libs/module-api-1.0.0.jar"))
}

tasks {
    shadowJar {
        archiveFileName = "tpa-module.jar"
        manifest {
            attributes["Module-Main"] = "de.reddev.tpamodule.TpaModule"
        }
    }
    build { dependsOn(shadowJar) }
    jar { enabled = false }
}
