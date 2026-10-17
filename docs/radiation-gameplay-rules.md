# Radiation gameplay rules

Proposed first-release rules for the Hytale port, ready for review and subsequent
implementation. Preserve the datapack's survival loop: short unprotected trips,
substantial shelter, a complete protective suit, and treatment for emergencies.
Numbers marked as Hytale adaptations are initial balance choices to playtest.
This specification does not describe the current prototype's behavior.

## Minecraft reference

Reference snapshot: the clean local Minecraft repository at commit
`6da14fb191c912fb88c186584c1eb6237351e16e`.
Paths below are relative to `solar-radiation/data/sr/` in that repository.

| Mechanic | Datapack behavior | Hytale decision |
| --- | --- | --- |
| Dose | 0 to 180; +1 per exposed second, -1 per safe second | Retain, with fractional dose |
| Stages | Thresholds 10, 30, 60, 90, 120, 180 | Retain; healthy stage is 0 |
| Shelter | Three qualifying blocks anywhere in the vertical column; excludes leaves and ordinary glass, includes tinted glass | Retain thickness; define shielding materials explicitly |
| Suit | All four hazmat pieces required; pieces lose durability | Retain complete-set protection; normalize wear |
| Spread | Players at dose 5+ infect vulnerable mobs within 5 blocks; infected mobs seed others within 2 blocks | Retain direction and distances; prevent instant chain propagation |
| Mob symptoms | Burning and glowing | Replace with the existing low-tier poison effect and its visuals |
| Treatment | Drinkable and splash potions clear dose; refreshed symptoms can linger | Clear dose and radiation-owned symptoms immediately |
| Multiplayer | Four-player bossbar allocation | No gameplay player limit |

Sources: [constants](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/install.mcfunction),
[one-second loop](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/main_loop.mcfunction),
[radiation stages](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/scripts/radiation/radiation_manager.mcfunction),
[shelter scan](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/scripts/raycast.mcfunction),
[equipment](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/scripts/equipment_handler.mcfunction),
[spread](https://github.com/NicolaMigliore/mc-datapack-solar-radiation/blob/6da14fb191c912fb88c186584c1eb6237351e16e/solar-radiation/data/sr/function/scripts/rad_transfer/rad_transfer_main.mcfunction).

## Dose and recovery

- Dose is a finite value clamped to **0 through 180**. Store dose; derive the
  stage from it. Do not round dose before comparing thresholds.
- An unprotected player gains **1 dose per simulation second** while exposed
  to solar radiation. Otherwise they lose **1 dose per simulation second**,
  immediately, down to zero. A complete suit permits recovery even in sunlight.
- At zero dose, stage is 0. Crossing a threshold applies or removes its symptoms
  within the next gameplay update. Stage 0 includes small nonzero doses below 10.
- Recovery reduces dose, not lost health. Normal healing remains separate.
- World time determines daylight; elapsed simulation time determines dose.
  Changing the clock or sleeping does not grant skipped-time exposure/recovery.
  Low server tick rates must not multiply rates by the number of ticks.

From zero, continuous unprotected exposure reaches the first symptoms after
10 seconds, poisoning damage after 120 seconds, and stronger poisoning after
180 seconds. Recovering from maximum dose to zero takes 180 safe seconds.

## Player stages and symptoms

Keep the datapack's thresholds, but use only existing Hytale effect mechanics for
the first release. The mappings below are **Hytale adaptations**, checked against
the installed 0.7.2 assets. Each row lists the complete active effect set.
Intermediate stages increase warning severity without introducing another debuff.

| Stage | Dose range | Active effects and feedback |
| --- | --- | --- |
| 0 Healthy | 0 <= dose < 10 | No symptoms |
| 1 Irradiated | 10 <= dose < 30 | `Slow`; initial exposure warning |
| 2 Elevated | 30 <= dose < 60 | `Slow`; elevated dose warning |
| 3 High | 60 <= dose < 90 | `Slow`; high dose warning |
| 4 Severe | 90 <= dose < 120 | `Slow`; imminent poisoning warning |
| 5 Poisoned | 120 <= dose < 180 | `Slow` and `Poison_T1` |
| 6 Critical | dose = 180 | `Slow` and `Poison_T2`; replaces the weaker poison |

Use the stock definitions under `Server/Entity/Effects/Status/` as templates:

- `Slow.json`: horizontal speed multiplier **0.5**, with its existing tint,
  model VFX, and snow screen overlay. Accept these frost-themed visuals initially;
  a radiation-specific visual redesign is deferred.
- `Poison_T1.json`: **6 base poison damage every 5 seconds**.
- `Poison_T2.json`: **12 base poison damage every 5 seconds**.
  Both poison tiers inherit visuals and other behavior from `Poison.json`.
  These are configured base values; native defenses and damage processing decide
  the actual damage. There is no custom armor bypass or nonlethal health floor.
- Do not use `Poison_T3` as a stronger damage tier: its base damage matches T2;
  its stock duration is longer.

Create radiation-specific copies of these definitions with dedicated IDs, keeping
their gameplay values and native mechanics unchanged. This small amount of asset
configuration allows radiation to own and remove its effects without clearing an
unrelated poison or slow effect. It does not add a new status-effect mechanic.
Do not globally override the game's stock effect assets.

Maintain one radiation slow effect and at most one radiation poison tier per
entity. Refresh duration as needed while the stage applies, without reapplying
every tick or restarting the poison damage interval. Replace the poison tier on
stage changes; remove radiation-owned effects when no longer justified. Test
coexistence with effects from other sources rather than assuming how they stack.

Mining/gathering slowdown, stamina-regeneration penalties, forced nausea,
percentage-based damage, custom damage floors, and bespoke radiation markers are
deferred. Add no custom camera movement or flashing. Use the existing effect
visuals and planned HUD for feedback. Mob symptoms are defined separately below.

## Daylight and shelter

- Solar exposure requires a world with solar radiation enabled, daytime in that
  world's day/night cycle, insufficient overhead shielding, and no complete suit.
  Use the world's day/night transition, rather than Minecraft tick values or a
  global clock. Sunrise begins exposure; sunset ends it.
- Radiation is enabled by default in ordinary surface worlds. Worlds without a
  day/night cycle require explicit opt-in and an explicit daylight setting.
- Night, caves, and valid shelters stop solar input. Weather, artificial light,
  shade from terrain to the side, and immersion in water do not change solar input.
- Evaluate the vertical column above the entity's head. **Three full shielding
  blocks** anywhere above it provide shelter, including separated layers.
  One or two do not. A roof works without enclosing walls.
- Full opaque terrain and construction blocks count by default. Air, fluids,
  leaves, ordinary glass, and partial blocks do not. A specifically tagged
  radiation-shielding glass may count. Explicit shielding tags take precedence;
  map actual Hytale block assets when implementing exposure.
- Sheltering follows the entity's current head position. Reevaluate movement,
  roof changes, world transfers, and day/night transitions within one second.
  An unavailable block column is unknown: pause solar input and recovery until
  it can be evaluated, rather than treating unloaded terrain as a safe bunker.
- Sleeping and disconnecting do not reset dose. Shelter blocks sunlight only;
  nearby contamination has its own rules.

## Hazmat protection

- All four equipped hazmat pieces must be present and have positive durability
  to block **100% of solar dose**. Partial sets and ordinary armor give no
  radiation protection in the first release. Cosmetic appearance is insufficient.
- Each equipped hazmat piece loses **1/180 of its maximum durability per second**
  while the player is in daytime with insufficient shelter. A new complete suit
  lasts about three minutes of solar exposure. Fractional wear carries forward.
  This normalized lifetime is a Hytale balance choice.
- Partial sets also wear in those conditions. There is no radiation-related wear
  at night or under valid shelter. Ordinary combat wear still applies.
- If any piece breaks or is removed, protection ends at the next update. Warn
  at 20% durability and when the complete seal is lost.
- A suit does not remove existing dose, suppress symptoms, or prevent an already
  contaminated player from spreading contamination. Dose must recover or be treated.

## Contamination

Retain the datapack's asymmetric spread. **Players acquire dose from sunlight;
they do not acquire dose from other players or mobs in the first release.**
Ordinary mobs do not acquire dose directly from sunlight.

- A living survival player at **dose >= 5** can contaminate eligible mobs within
  **5 world units**, including while sheltered, suited, or outside at night.
- Eligible mobs are living passive animals and neutral civilian NPCs in an
  explicit susceptible asset group. Hostile creatures, undead, constructs,
  bosses, dropped items, and corpses are excluded by default. New NPC assets
  require explicit classification; unknown types are immune.
- A clean eligible mob receives **10 dose** on first contact. While within range
  of an infectious player, it receives **2 dose per second**, while also losing
  **1 dose per second** naturally. Dose remains capped at 180.
- A contaminated mob with **dose > 0** seeds clean eligible mobs within
  **2 world units** with 10 dose. Mob sources do not top up existing contamination.
  Away from players, a newly seeded mob recovers in approximately ten seconds.
- Run transmission once per second using the sources present at the beginning
  of that update. Newly infected mobs first spread on the next update. Multiple
  sources do not multiply incoming dose; seed a target only once per update.
  Initial seeding takes precedence over top-up, and the one-dose natural decay
  occurs after seeding/top-up. Thus first-contact mob dose is 9 after that update.
- Distances include their boundary and are measured between entity positions
  in the same world. **Hytale adaptation:** require an unobstructed straight path
  between source and target; solid walls prevent transmission. Sun shielding
  thickness is not required for this barrier.
- Any contaminated mob receives the radiation-owned copy of `Poison_T1`, with
  its native damage and visuals. It does not receive player `Slow` or stronger
  poison tiers. There is no nonlethal guarantee. This replaces Minecraft's
  burning behavior; contamination does not ignite terrain or spread fire.
  Clear the radiation-owned poison at zero dose.

## Decontamination

- A completed drinkable treatment clears the user's dose to zero. A splash
  treatment clears living players and eligible mobs within **3 world units**
  of impact, with an unobstructed path from impact to target. The splash radius
  is a Hytale adaptation. One completed use consumes one item.
- Remove radiation-owned symptoms immediately. Do not restore health, repair
  equipment, clear unrelated effects, or grant temporary immunity.
- Treatment resolves before the next transmission snapshot and dose update.
  Remaining in sunlight or beside an infectious player can contaminate again.
- Retain the unfinished treatment ingredient as an optional progression item:
  consuming unstable solution adds **30 dose**, capped at 180, as in the source.
  Actual ingredients, workstations, and recipes belong to the content issues.

## Persistence and player feedback

- Save dose through logout, server restart, and world transfer. Do not simulate
  radiation or recovery while an entity is offline or unloaded. Recompute exposure
  and symptoms on load; do not trust a persisted exposure flag.
- **Hytale adaptation:** death clears dose on respawn. Dead entities and corpses
  neither receive dose nor spread it. Creative/spectator players are exempt from
  automatic dose changes, symptoms, equipment wear, and transmission; entering
  these modes does not erase stored dose.
- Show dose, stage, current solar exposure, and suit status in the HUD. Warn that
  contamination can spread from dose 5, before stage 1 symptoms appear. Display
  fractional dose consistently; stage 0 must not imply the dose is necessarily zero.
- Debug commands report the resulting dose and derived stage. The current named
  amount syntax is `/radiation set --amount=100`; gameplay updates resume normally
  after a debug change.
- Expose dose rates, stage thresholds/effect mappings, shielding depth/tags, suit lifetime,
  susceptibility groups, spread radii, and treatment radius as validated settings.
  Defaults above are the first-release baseline. No player-count gameplay cap.

## Acceptance scenarios for implementation

1. From zero dose, ten exposed simulation seconds reach stage 1; at 30, 60, 90,
   120, and 180 dose, the corresponding stage applies without integer rounding.
2. At night and beneath three shielding blocks, dose falls by one per second.
   Beneath one or two blocks, or ordinary glass/leaves, daytime dose rises.
3. Different worlds use their own clocks. A time skip changes exposure without
   applying hours of instant dose or recovery. Unknown terrain grants neither.
4. A complete intact suit blocks solar input, allows recovery, and wears only in
   unshielded daytime. Removing/breaking one piece restores solar input.
5. Dose 4.99 does not spread; dose 5 does. Test exact radii, solid walls, two
   simultaneous sources, and a chain of mobs without same-update cascading.
6. A mob seeded to 10 finishes that transmission update at 9; an existing mob
   near an infectious player gains a net one per second. Neither infects players.
7. Stages 1 through 4 apply the same stock slow mechanics; stage 5 adds low-tier
   poison and stage 6 replaces it with the stronger tier. Verify native poison
   damage, defenses, death behavior, and refresh timing in game. Falling below a
   threshold removes only radiation-owned symptoms, without deleting other effects.
8. Treatment clears dose and symptoms without healing; continued exposure can
   restart accumulation. Relog/restart retains dose; respawn clears it.
9. Verify partial suits, multiple players, NPC exclusions, low tick rates, and
   world transfers. These scenarios validate future implementation; they are not
   claims that the current prototype already passes.
