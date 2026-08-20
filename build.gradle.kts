plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "8.3.8"
    id("io.freefair.lombok") version "8.14"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly(libs.paper.api)
    // Optional at runtime: placeholders are only resolved when it is installed.
    compileOnly(libs.placeholderapi)
    implementation(libs.nexus)

    // Redis dependency
    implementation(libs.jedis)

    // Gson for JSON serialization
    implementation(libs.gson)

    // paper-t api is compileOnly for the plugin, buthe tests need it on their runtime classpath
    // to load classes whose signatures mention Bukkit types.
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("1.21.8")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        // Relocate conflict-prone libraries so other plugins' copies can't clash with ours.
        relocate("com.google.gson", "org.glstudio.chat.libs.gson")
        relocate("redis.clients.jedis", "org.glstudio.chat.libs.jedis")
        mergeServiceFiles()
    }
}
