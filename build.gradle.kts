import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.comet"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target IntelliJ IDEA Community 2024.3 (works in Ultimate too).
        intellijIdeaCommunity("2024.3")
        // Bundled Git plugin — exposes the git4idea command API we drive.
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }

    // Verify against the build-target IDE (already cached) — `./gradlew verifyPlugin`
    // reports compatibility problems and dynamic-plugin (no-restart) eligibility.
    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3")
        }
    }
}

kotlin {
    jvmToolchain(21)
}
