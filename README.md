# Solar Radiation for Hytale

A server plugin that makes sunlight hazardous and encourages shelter, nighttime
exploration, and preparation. Based on the original
[Solar Radiation Minecraft datapack](https://modrinth.com/datapack/solar-radiation).
The current implementation is an early prototype.

## Pinned development baseline

| Component | Version | Configured in |
| --- | --- | --- |
| Hytale release server/API | 0.7.2 | `gradle.properties` |
| Gradle | 9.2.0 | `gradle/wrapper/gradle-wrapper.properties` |
| Java language and bytecode | 25 | `build.gradle.kts` |

Use an installed **JDK 25** and the committed Gradle wrapper. No system Gradle,
ScaffoldIt, or special JetBrains runtime is required. The wrapper distribution
has a pinned SHA-256 checksum. The plugin manifest receives the exact server
constraint `=0.7.2` during resource processing.

## Build

From PowerShell in the repository root:

```powershell
java -version
.\gradlew.bat --version
.\gradlew.bat clean build --console=plain
```

The plugin is written to `build/libs/solar-radiation.jar`. Building downloads the
pinned server API from Hytale's Maven repository; it does **not** require a local
game installation or `Assets.zip`. There are currently no automated tests, so
`build` verifies compilation and packaging.

On Linux/macOS, use `./gradlew` in place of `.\gradlew.bat`. In IntelliJ, import
the Gradle project, select the wrapper, and set the Gradle JVM to JDK 25.

## Local server

Install the matching Hytale client/assets through the launcher. With a standard
installation, the task locates `Assets.zip` under your Hytale application data
folder. For this machine's custom installation, run:

```powershell
.\gradlew.bat runServer "-Phytale.home_path=G:/Hytale" --console=plain
```

For a permanent machine-specific setting, put this in your **user** Gradle
properties (`%USERPROFILE%/.gradle/gradle.properties` on Windows,
`~/.gradle/gradle.properties` elsewhere), outside this repository:

```properties
hytale.home_path=G:/Hytale
```

Alternatively, supply an explicit matching asset archive:

```powershell
.\gradlew.bat runServer "-Phytale.assets=D:/HytaleDev/0.7.2/Assets.zip" --console=plain
```

`runServer` builds the plugin, copies its JAR into `devserver/mods`, and starts
the pinned Maven server on **127.0.0.1:5520** with a maximum 4 GB Java heap.
`setupServer` only builds and copies the plugin. Configuration, saves, logs,
and authentication state stay in the ignored `devserver` directory. Existing
worlds and other mods are preserved; use a fresh directory when first checking
this baseline. Remove any older Solar Radiation JARs from that directory before
using it, so only one copy of the plugin loads.

For authenticated play, follow the console's `/auth login` instructions and
connect with the matching client to `127.0.0.1:5520`. Check the log for both
`Enabled plugin io.github.nicolamigliore:SolarRadiationPlugin` and
`Hytale Server Booted!`. Enter `stop` in the server console for a clean shutdown.
Stop the server, rebuild, and restart after code changes; hot reload is not
configured. In game, `/radiation stats`, `/radiation add`, and `/radiation set`
are the existing debug commands.

## Isolated startup check

Use a fresh disposable world under `build/` and opt into insecure authentication
for a local startup check without a server login:

```powershell
.\gradlew.bat runServer "-Phytale.home_path=G:/Hytale" "-Phytale.serverDir=build/startup-check" "-Phytale.insecure=true" --console=plain
```

The server remains bound to localhost. Wait for the plugin-enabled and server
booted messages, then enter `stop`. Inspect `build/startup-check/logs/` for
plugin errors. This verifies server startup and registration; client connection
and radiation gameplay still require an in-game check. `clean` removes this
startup-check world; it leaves `devserver` intact.

Verified on Windows with JDK 25.0.2 on 16 October 2026: clean build, packaged
manifest constraint, plugin enablement, server ready on localhost, and clean
shutdown. The existing `devserver` world was not used. The prototype still emits
a deprecation note for its exposure system; the base game also emits asset
warnings during startup.

## Troubleshooting and upgrades

- **Wrong Java:** point `JAVA_HOME` and your IDE's Gradle JVM at a JDK 25 installation.
- **Missing assets:** set `hytale.home_path` to the folder containing `install`,
  or set `hytale.assets` directly. Use forward slashes in Java properties files.
  Flatpak/custom installations should use one of these explicit overrides.
- **Launcher updated:** the task checks the adjacent `Server/HytaleServer.jar`
  version, when present, and refuses assets from a different installed release.
  Keep a versioned copy of the matching assets before upgrading. An explicit
  standalone archive must be matched manually.
- **Client cannot connect:** use a client matching the pinned server release.
  Authentication and in-game verification are separate from the startup check.
- **Port already in use:** stop the other local server before starting this one.

To upgrade Hytale, change `hytale.version` deliberately, obtain matching client
and assets, then repeat the clean build and isolated startup check. Gradle can
be upgraded separately with the wrapper task; update its distribution checksum
from the official Gradle distribution alongside the version. Commit wrapper
files together. Keep game assets, server credentials, and generated worlds out
of Git.

References: [Hytale server manual](https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual),
[Hytale release artifacts](https://maven.hytale.com/release/com/hypixel/hytale/Server/maven-metadata.xml),
[Gradle wrapper documentation](https://docs.gradle.org/9.2.0/userguide/gradle_wrapper.html).
