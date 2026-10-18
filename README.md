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

Verified on Windows with JDK 25.0.2 on 18 October 2026: build and 101 tests,
including native HUD-manager/packet tests, effect escalation, recovery, and
unrelated-effect preservation. An isolated startup check verified
plugin and effect-pack loading, server ready on localhost, and clean shutdown.
Existing development saves were not used. The base game emits asset warnings
during startup. Player-facing effects and damage timing still need an in-game
check with a connected client. HUD layout and UI scaling also require that check;
the server does not render or parse the client UI layout.

## Radiation gameplay and testing

A radiation HUD appears automatically once the client is ready. A slim, permanently
green bar with a radiation icon sits just above the native health bar, with its
icon and left edge aligned to health. Its frame and fill match health's 318x12
dimensions, using the same centered 702px hotbar container. Its beveled frame and shaded fill follow the native health/stamina
bar proportions. It fills from 0 to 180 and drains during recovery. There is no
numeric dose counter or large panel; stage followed by exposure appears on the
same row to the right of the bar. Use `/radiation stats` for exact dose. Stage uses the exact dose even when
the bar moves less than one pixel. Zero dose remains visible; checking terrain, disabled worlds, exempt
game modes, and death each have an explicit status. Mobs never receive a HUD.

The HUD refreshes at most four times per simulation second and sends only changed
values. The native `ProgressBar` receives a normalized value only when its visible
pixel width changes; its green texture stays fixed at every stage. Stable values
send no packets after the initial layout and any transition highlight has settled.
A second native progress-bar layer uses the matching client's health-bar highlight
texture (`BarEffect.png`). Dose changes show a brief highlight; during recovery it
trails the green fill and settles over 0.75 seconds. Both layers share the same
four-updates-per-second limit. Death, exempt/disabled states, and unknown terrain
cancel the highlight. The display uses
its own `SolarRadiation:Status` HUD key, preserving native HUD elements and other
mods' HUDs. It opens no page and requires no command. Stage chat messages are
replaced by the HUD; the early warning at dose 5 and debug commands remain.

After restarting, verify the panel during ordinary movement, shelter/night
transitions, and `/radiation set --amount=140`. Check recovery through 120 and 10,
Creative/Adventure switches, death/respawn, relog, and world transfer. Expect one
panel with current values after each transition; compare it with `/radiation stats`.
Check placement and legibility at your normal UI scale. Hazmat suit status will
be added with equipment support.

The replaceable icon is
`src/main/resources/Common/UI/Custom/Hud/SolarRadiation/RadiationIcon.png`: a
transparent 64x64 PNG displayed at 24x24. `BarBackground.png`, `BarFill.png`, and
`BarEffect.png` in the same folder can also be edited directly. Rebuild/restart
after PNG changes; normal builds package these PNGs without regenerating them.

`BarEffect.png` uses the vanilla health-bar effect. Adjust the HUD document's `Bottom` anchor to
tune vertical placement for your UI scale.

The player loop currently includes dose clamped to 0 through 180, stages 0 through
6, per-world day/night detection, three full opaque blocks of overhead shelter,
and configurable exposure/recovery rates (both default to one dose per simulation
second). Unknown terrain pauses dose changes.
Creative mode pauses dose and suppresses symptoms; switch to Adventure mode to
test normal gameplay. Death clears dose. Saved dose survives relog/restart and
world transfers, including older saves; exposure is recomputed after loading.

Stages 1 through 4 apply the stock Slow mechanics. Stage 5 adds Poison T1; stage
6 replaces it with Poison T2. Dedicated effect IDs isolate radiation cleanup from
other slow/poison effects. Effects are maintained without resetting damage timers.
Stock Slow has a 50% speed multiplier and frost-themed visuals.
Poison T1 deals 6 base poison damage every 5 seconds; T2 deals 12 every 5 seconds.
Native defenses and damage rules determine the final damage; radiation does not
write health directly or impose a nonlethal floor. Replacing a poison tier starts
that tier's native cooldown; keeping the same tier preserves its timer.

Dose and symptom updates run before Hytale's native effect tick. Falling below a
threshold removes the outgoing effect completely, without its remaining duration
lingering. Death and respawn also clear radiation-owned effects immediately.
Startup validates that radiation assets are infinite, support complete removal,
and define poison damage with a positive cooldown.

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

Check stage changes in both directions: expose a player just below 10, 30, 60,
90, 120, or 180 dose, then take shelter after crossing the threshold. Stage 6
downgrades from Poison T2 to T1 below 180; poison clears below 120 and Slow clears
below 10. A fast rate or long tick can skip stages; symptoms use the final dose.
Recovery removes symptoms but does not restore lost health.

For repeatable damage checks, temporarily set both dose rates below to 0 and
restart; this keeps each debug dose stable. In Adventure mode, compare dose 140
(T1) with 180 (T2), allowing at least five seconds for a damage pulse. Set 119 to
clear poison, then 9 to clear Slow. Check that an unrelated poison/slow source
survives radiation cleanup, Creative mode suppresses radiation symptoms, and
death/respawn clears dose and radiation effects. Restore the desired rates and
restart afterward. These connected-client checks cover actual health changes,
native defenses, visuals, and movement; automated tests cover effect-controller
calls and preservation of active effect instances.

The server creates `mods/io.github.nicolamigliore_SolarRadiationPlugin/Radiation.json`
inside the selected server directory. Edit it while the server is stopped:

```json
{
  "Enabled": true,
  "DisabledWorlds": [],
  "ExposureDosePerSecond": 1.0,
  "RecoveryDosePerSecond": 1.0,
  "ShelterDepth": 3,
  "ExposureCheckIntervalSeconds": 0.5,
  "ShieldingBlocks": [],
  "NonShieldingBlocks": [],
  "ShieldingTags": [],
  "NonShieldingTags": ["Family=Leaves"]
}
```

`DisabledWorlds` contains exact world names; disabled worlds freeze dose and
suppress symptoms. Exclude instances/worlds without a solar cycle here as needed.
Older configuration files receive the new defaults automatically. `ShelterDepth`
must be at least 1. `ExposureCheckIntervalSeconds` must be finite, greater than
zero, and at most 1. Stage thresholds remain 10, 30, 60, 90, 120, and 180.

`ExposureDosePerSecond` controls player solar accumulation. `RecoveryDosePerSecond`
controls safe-player and opted-in mob recovery. Both must be finite and nonnegative;
zero disables that change without clearing existing dose or symptoms. For a quick
rate check, set exposure to 2 and recovery to 0.5, restart, and compare stats over
ten simulation seconds: roughly 20 dose gained outside or 5 lost in shelter/night.
Keep away from the 0/180 limits for this check. Rates use elapsed simulation time,
independently of the terrain sampling interval; world-clock skips do not add dose
or recovery for the skipped hours.

`ShieldingBlocks` and `NonShieldingBlocks` accept exact, case-sensitive block asset
IDs. `ShieldingTags` and `NonShieldingTags` accept native Hytale tags such as
`Type=Rock` or `SolarRadiation=Shielding`; a tag key alone matches any of its
values. Tags must exist on the relevant assets. The leaf-family exclusion also
handles custom opaque leaf cubes.

Material precedence is: excluded ID, included ID, excluded tag, included tag,
then the defaults. Thus exact IDs can make exceptions to broad tag rules;
exclusion wins at equal specificity. Air/empty material never shields. By
default, full opaque cubes with the `Full` hitbox shield; transparent/cutout
blocks, leaves, model blocks, slabs, and fences do not. Add a particular glass
block's ID to `ShieldingBlocks` to make it count as shielding.

Daylight follows the current world's game clock and Hytale's sunrise/day-length
constants. Sunrise includes its first second; sunset ends exposure. Day/night
changes take effect on the next gameplay tick, without terrain scans at night.
Weather and visual shadows do not affect exposure.

Position and roof changes are sampled at the configured interval, including
while moving. This prevents fast movement from forcing scans every tick. Scans
stop at the terrain/collision heightmap bound or after enough shielding layers.
They reuse section lookups, skip entire known-air and missing 32-block sections,
and classify each encountered block type once per scan. Missing sections remain
unknown, not air: unresolved columns pause dose changes unless enough known
shielding already proves shelter. Material decisions are not cached between
scans, so roof and asset changes are picked up on the next check.

## Radiation ECS foundation

`RadiationComponent` contains persistent dose only, using the original
`SolarRadiation_EntityData` registration and `TotalRadiations` save key. Stages and
progress are derived from dose. `RadiationRuntimeComponent` contains exposure and
the exposure-check timer. It is registered without a
serialization codec and excluded from serializable clones. Loading or transferring
an entity creates fresh runtime state without changing dose.

`RadiationHudSystem` runs on each player's world thread after the dose update,
waiting for client readiness. HUD state belongs to the player's native HUD manager
and is never serialized. Leaving a world or disconnecting removes the owned HUD;
the destination world creates it from current dose/exposure once ready. Missing
dose removes it, native HUD resets rebuild it, and plugin shutdown schedules owned
HUD cleanup on the relevant world threads. There is no global player cache or
background task to retain disconnected players.

Players receive dose data automatically. Mobs opt in only when another system
or a debug command attaches `RadiationComponent`; ordinary mobs are unaffected.
The shared loop processes any entity carrying dose data without requiring player
components. Opted-in mobs recover at the configured rate (one dose per second by
default) and receive low-tier poison at any positive dose. They never acquire
solar dose, player Slow, stronger poison,
or player chat messages. Death clears dose for both players and mobs.

To test a mob, look directly at it within 10 blocks and run:

```text
/radiation set --target --amount=20
/radiation stats --target
/radiation set --target --amount=0
```

Expect `RECOVERING`, dose lost at the recovery rate even in sunlight, low-tier
poison without Slow, and radiation-owned poison removed at zero. The same flag
works with `add` and can target another player. Dose changes still require
`solarradiation.admin` or OP. Automatic spread and susceptibility selection remain
part of the contamination issue.

When adding future radiation sources, attach the persistent component on the
entity's world thread using its store or command buffer. Lifecycle systems create
runtime state for new/load/late-added dose components and remove runtime plus
radiation-owned effects when the dose component is removed. Updating/replacing
dose preserves runtime state. Use zero dose to decontaminate an entity while
retaining its radiation capability.

Hazmat equipment, treatment items, and automatic mob contamination are not yet
implemented. The radiation HUD, early warning, and debug stats provide feedback.

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
