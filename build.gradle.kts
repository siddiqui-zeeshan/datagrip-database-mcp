plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.intellijPlatform)
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(17)
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create(
            providers.gradleProperty("platformType").get(),
            providers.gradleProperty("platformVersion").get(),
        )

        bundledPlugins(
            "com.intellij.database",
            "com.intellij.mcpServer",
        )

        // instrumentationTools() is no longer necessary with recent plugin versions
    }

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }

        changeNotes = providers.gradleProperty("pluginChangeNotes")
    }

    signing {
        val certChainFile = providers.environmentVariable("CERTIFICATE_CHAIN_FILE")
        val privKeyFile = providers.environmentVariable("PRIVATE_KEY_FILE")
        val certChainContent = providers.environmentVariable("CERTIFICATE_CHAIN")
        val privKeyContent = providers.environmentVariable("PRIVATE_KEY")

        if (certChainFile.isPresent && privKeyFile.isPresent) {
            certificateChainFile = file(certChainFile)
            privateKeyFile = file(privKeyFile)
        } else if (certChainContent.isPresent && privKeyContent.isPresent) {
            certificateChain = certChainContent
            privateKey = privKeyContent
        }
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    instrumentCode = false
}

tasks {
    wrapper {
        gradleVersion = "8.13"
    }
}
