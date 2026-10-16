import java.util.jar.JarFile

plugins {
    java
}

group = "io.github.nicolamigliore"
version = "0.0.0"

val hytaleVersion = providers.gradleProperty("hytale.version").get()

repositories {
    mavenCentral()
    maven("https://maven.hytale.com/release") {
        content { includeGroup("com.hypixel.hytale") }
    }
}

val hytaleServer by configurations.creating

dependencies {
    compileOnly("com.hypixel.hytale:Server:$hytaleVersion")
    hytaleServer("com.hypixel.hytale:Server:$hytaleVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.jar {
    archiveFileName.set("solar-radiation.jar")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
}

tasks.processResources {
    inputs.property("hytaleVersion", hytaleVersion)
    filesMatching("manifest.json") {
        filter { line -> line.replace("\"ServerVersion\": \"*\"", "\"ServerVersion\": \"=$hytaleVersion\"") }
    }
}

val serverDirectory = layout.projectDirectory.dir(
    providers.gradleProperty("hytale.serverDir").getOrElse("devserver")
)
val userHome = System.getProperty("user.home")
val defaultHytaleHome = when {
    System.getProperty("os.name").lowercase().contains("win") -> "$userHome/AppData/Roaming/Hytale"
    System.getProperty("os.name").lowercase().contains("mac") -> "$userHome/Library/Application Support/Hytale"
    else -> "$userHome/.local/share/Hytale"
}
val hytaleHome = providers.gradleProperty("hytale.home_path").getOrElse(defaultHytaleHome)
val assetsFile = file(providers.gradleProperty("hytale.assets").getOrElse(
    "$hytaleHome/install/release/package/game/latest/Assets.zip"
))

val setupServer by tasks.registering(Copy::class) {
    group = "hytale"
    description = "Build and install this plugin into the local server's mods directory."
    from(tasks.jar)
    into(serverDirectory.dir("mods"))
}

tasks.register<JavaExec>("runServer") {
    group = "hytale"
    description = "Start a local Hytale server with the built plugin. Stop with /stop."
    dependsOn(setupServer)
    classpath = hytaleServer
    mainClass.set("com.hypixel.hytale.Main")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
    workingDir(serverDirectory)
    standardInput = System.`in`
    maxHeapSize = "4G"
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    args("--assets", assetsFile.absolutePath, "--bind", "127.0.0.1:5520", "--allow-op", "--disable-sentry")
    if (providers.gradleProperty("hytale.insecure").getOrElse("false").toBoolean()) {
        args("--auth-mode", "insecure")
    }
    doFirst {
        check(assetsFile.isFile) {
            "Assets.zip not found at $assetsFile. Set -Phytale.assets=/path/to/Assets.zip " +
                "or hytale.home_path in your user Gradle properties. Use assets matching Hytale $hytaleVersion."
        }
        val installedServer = assetsFile.resolveSibling("Server/HytaleServer.jar")
        if (installedServer.isFile) {
            val installedVersion = JarFile(installedServer).use {
                it.manifest.mainAttributes.getValue("Implementation-Version")
            }
            check(installedVersion == hytaleVersion) {
                "Installed Hytale $installedVersion does not match pinned $hytaleVersion. " +
                    "Set hytale.assets to a matching archived Assets.zip or deliberately update hytale.version."
            }
        }
    }
}
