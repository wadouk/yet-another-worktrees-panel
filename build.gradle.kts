import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Target platform read from gradle.properties (default IC 2024.3, works in Ultimate too).
        create(providers.gradleProperty("platformType").get(), providers.gradleProperty("platformVersion").get())
        // Bundled Git plugin — exposes the git4idea command API we drive.
        bundledPlugin("Git4Idea")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild").get()
            untilBuild = provider { null }
        }
    }

    // Verify against the build-target IDE (already cached) — `./gradlew verifyPlugin`
    // reports compatibility problems and dynamic-plugin (no-restart) eligibility.
    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaCommunity, providers.gradleProperty("platformVersion").get())
        }
    }
}

kotlin {
    jvmToolchain(21)
}
