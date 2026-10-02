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

A tag matching `mod_version` (for example `v0.3.0`) builds and publishes a GitHub Release. Clients and dedicated servers check the latest public release at startup. A newer stable `msr-<version>.jar` is downloaded and SHA-256 verified, then installed after the game or server closes. Restart to use it. Run `/msr update` in game or from the dedicated server console to check immediately. Automatic replacement is skipped in Gradle development runs.

## Economy and estates

Press **G** in game to open the custom estate dashboard. The server stores balances and owned chunks in the world save, so the GUI shows server-authoritative data.

| Inventory item | Sale price per ingot |
| --- | ---: |
| Iron ingot | 1,000 won |
| Gold ingot | 5,000 won |
| Netherite ingot | 100,000 won |

Each sale button sells **all** ingots of that type in the player's inventory. The current chunk costs **100,000 won** to claim. The dashboard shows its dimension, coordinates, owner, your balance, and number of owned chunks. Releasing a chunk does not refund the purchase price and requires a second click within five seconds.

Other players cannot break, place, or interact with blocks in a claimed chunk. Explosions cannot destroy claimed blocks; pistons and fluid block placement cannot cross ownership boundaries. Operators with permission level 2 may edit any chunk. Claims apply to all dimensions and persist across server restarts.

## Claim visibility

The estate dashboard leaves the world sharp behind it. Its chunk tile and the small HUD minimap sample real vanilla map terrain colors around you and mark owned chunks with a cyan diamond and your own chunk with a gold cross. Open **소유 청크 지도** from the dashboard to view a larger 17×17 chunk map with a compass frame; use the arrow keys or click the grid to pan, and the previous/next buttons to jump between owned chunks in the current dimension. A claim-entry message appears in the action bar when entering someone's land. Purple portal particles trace the borders of your nearby owned chunks.

If [Xaero's Minimap](https://modrinth.com/mod/xaeros-minimap) is installed, owned chunks are also added as waypoints on its real minimap and full map automatically. This integration reflects into Xaero's internal (unofficial) API, so it may silently stop working after a Xaero update; players without the mod are unaffected.
