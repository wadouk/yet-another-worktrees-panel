plugins {
    // Lets Gradle auto-provision the JDK 21 toolchain regardless of the JDK
    // running Gradle itself (handy on machines with only JDK 24 installed).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "yet-another-worktrees-panel"
