# MSR

Minecraft 1.21.1 / NeoForge mod with a required Create dependency, economy, and chunk estates.

## Requirements

- Java 21 JDK
- Internet access for the first Gradle sync

## Build and run

```sh
./gradlew build
./gradlew runClient
```

The built JAR is placed in `build/libs/`. Install Create 6.0.10 or a compatible 6.0.x release alongside this mod. The mod ID is `msr`; change it consistently in `gradle.properties` and `MsrMod.java` if needed.

## GitHub Releases and automatic updates

A tag matching `mod_version` (for example `v0.2.0`) builds and publishes a GitHub Release. Clients and dedicated servers check the latest public release at startup. A newer stable `msr-<version>.jar` is downloaded and SHA-256 verified, then installed after the game or server closes. Restart to use it. Run `/msr update` in game or from the dedicated server console to check immediately. Automatic replacement is skipped in Gradle development runs.

## Economy and estates

Press **G** in game to open the custom estate dashboard. The server stores balances and owned chunks in the world save, so the GUI shows server-authoritative data.

| Inventory item | Sale price per ingot |
| --- | ---: |
| Iron ingot | 1,000 won |
| Gold ingot | 5,000 won |
| Netherite ingot | 100,000 won |

Each sale button sells **all** ingots of that type in the player's inventory. The current chunk costs **100,000 won** to claim. The dashboard shows its dimension, coordinates, owner, your balance, and number of owned chunks. Releasing a chunk does not refund the purchase price and requires a second click within five seconds.

Other players cannot break, place, or interact with blocks in a claimed chunk. Explosions cannot destroy claimed blocks; pistons and fluid block placement cannot cross ownership boundaries. Operators with permission level 2 may edit any chunk. Claims apply to all dimensions and persist across server restarts.
