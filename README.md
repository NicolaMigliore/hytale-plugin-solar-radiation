# Solar Radiation for Hytale

A server plugin that makes sunlight hazardous and encourages shelter, nighttime
exploration, and preparation. Based on the original
[Solar Radiation Minecraft datapack](https://modrinth.com/datapack/solar-radiation).
The current implementation supports the core player radiation loop.

The proposed first-release [radiation gameplay rules](docs/radiation-gameplay-rules.md)
define the target mechanics and their Minecraft reference. Implementation is tracked
separately; these rules are not all present in the prototype yet.

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
game installation or `Assets.zip`. `build` runs the automated dose, shelter,
persistence-codec, and native world-clock tests as well as compilation and
packaging. The HTML report is in `build/reports/tests/test/index.html`.

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

Verified on Windows with JDK 25.0.2 on 17 October 2026: clean build and 22 tests,
plugin and effect-pack loading, server ready on localhost, and clean shutdown.
Existing development saves were not used. The base game emits asset warnings
during startup. Player-facing effects and damage timing still need an in-game
check with a connected client.

## Radiation gameplay and testing

The player loop currently includes dose clamped to 0 through 180, stages 0 through
6, per-world day/night detection, three full opaque blocks of overhead shelter,
and one dose per second of exposure/recovery. Unknown terrain pauses dose changes.
Creative mode pauses dose and suppresses symptoms; switch to Adventure mode to
test normal gameplay. Death clears dose. Saved dose survives relog/restart and
world transfers, including older saves; exposure is recomputed after loading.

Stages 1 through 4 apply the stock Slow mechanics. Stage 5 adds Poison T1; stage
6 replaces it with Poison T2. Dedicated effect IDs isolate radiation cleanup from
other slow/poison effects. Effects are maintained without resetting damage timers.
Stock Slow has a 50% speed multiplier and frost-themed visuals.

After authenticating and joining the server, use `/op self` if you need operator
permissions. Dose-changing debug commands require `solarradiation.admin` (or OP).

```text
/radiation stats
/radiation set --amount=20
/radiation set --amount=140
/radiation set --amount=180
/radiation set --amount=0
/radiation add --amount=10.5
```

Use 20 dose to check Slow and 140 for low-tier poison. To check the critical tier,
set 180 while exposed in daytime; in shelter/night it immediately recovers below
180. Stats show dose, stage, exposure reason, and progress as a percentage.
Amounts are clamped to 0 through 180; nonfinite inputs are rejected. `set` requires
`--amount`; `add` defaults to 50 when omitted.

Check an open column in daytime (dose rises), then a roof with one/two full stone
blocks (still rises) and three layers (recovers). The layers may be separated.
Glass, leaves, and partial blocks do not shield by default. Changing the world
to night should allow recovery without advancing dose for the skipped hours.
After `/radiation set --amount=0`, radiation-owned effects should clear on the
next update. Relog/restart at a nonzero dose to check persistence.

The server creates `mods/io.github.nicolamigliore_SolarRadiationPlugin/Radiation.json`
inside the selected server directory. Edit it while the server is stopped:

```json
{
  "Enabled": true,
  "DisabledWorlds": [],
  "ShieldingBlocks": [],
  "NonShieldingBlocks": []
}
```

`DisabledWorlds` contains exact world names; disabled worlds freeze dose and
suppress symptoms. Exclude instances/worlds without a solar cycle here as needed.
`ShieldingBlocks` and `NonShieldingBlocks` accept exact block asset IDs; exclusions
win if an ID appears in both lists. These provide explicit material overrides;
tag-based shielding and configurable rates/thresholds are future work.

Hazmat equipment, treatment items, mob contamination, and the HUD are not yet
implemented. Stage/exposure messages and debug stats provide feedback for now.

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
