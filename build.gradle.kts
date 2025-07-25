import java.io.BufferedReader

plugins {
    kotlin("jvm") version "2.1.21" // Import kotlin jvm plugin for kotlin/java integration.
    id("com.diffplug.spotless") version "7.0.4" // Import auto-formatter.
    id("com.gradleup.shadow") version "8.3.6" // Import shadow API.
    eclipse // Import eclipse plugin for IDE integration.
}

val commitHash =
    Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=10", "HEAD")).let { process ->
        process.waitFor()
        val output = process.inputStream.use { it.bufferedReader().use(BufferedReader::readText) }
        process.destroy()
        output.trim()
    }
    
java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

kotlin { jvmToolchain(17) }

group = "net.trueog.quests-og" // Declare bundle identifier.

version = "$apiVersion-$commitHash"

val apiVersion = "1.19" // Declare minecraft server target version.
repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.purpurmc.org/snapshots") }
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://oss.sonatype.org/content/groups/public/") { name = "sonatype" }
    maven("https://jitpack.io") { name = "jitpack" }
}

dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") // Declare purpur API version to be packaged.
    compileOnly("net.luckperms:api:5.5") // Import LuckPerms API.
    implementation("org.jetbrains.kotlin:kotlin-stdlib") // Import Kotlin standard library.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") // Import Kotlin async library.
    implementation("io.lettuce:lettuce-core:6.7.1.RELEASE") // Import Lettuce API for keydb.
    compileOnly("com.github.Realizedd.Duels:duels-api:3.5.1") // Import Duels API (API-compatible with Duels-OG).
    compileOnly(project(":libs:Utilities-OG")) // Import TrueOG Network Utilities-OG API.
    compileOnly(project(":libs:DiamondBank-OG")) // Import TrueOG Network DiamondBank-OG API.
}

configurations.all { exclude(group = "io.projectreactor") }

val targetJavaVersion = 17

kotlin { jvmToolchain(targetJavaVersion) }

tasks.build {
    dependsOn(tasks.spotlessApply)
    dependsOn(tasks.shadowJar)
}

tasks.jar { archiveClassifier.set("part") }

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()
}

tasks.processResources {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }

    from("LICENSE") { into("/") }
}

tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    kotlin { ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts")
    }
}
