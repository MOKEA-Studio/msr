# MSR

Minecraft 1.21.1 / NeoForge mod scaffold with a required Create dependency.

## Requirements

- Java 21 JDK
- Internet access for the first Gradle sync

## Build and run

```sh
./gradlew build
./gradlew runClient
```

The built JAR is placed in `build/libs/`. Install Create 6.0.10 or a compatible 6.0.x release alongside this mod. The mod ID is `msr`; change it consistently in `gradle.properties` and `MsrMod.java` if needed.

This scaffold registers no content yet. Add registrations in `src/main/java/kr/mokea/msr/`.

## GitHub Releases and automatic updates

A tag matching `mod_version` (for example `v0.1.0`) builds and publishes a GitHub Release. The client checks the latest public release at startup. A newer stable `msr-<version>.jar` is downloaded and SHA-256 verified, then installed after Minecraft closes. Restart Minecraft to use it. The updater is skipped in development runs and on dedicated servers.
