# Athysrock's Pet Harnesses

Pet Harnesses is a Fabric mod for Minecraft Java 26.2. It adds `pet_harnesses:pet_harness`, a non-stacking, leggings item that can be used to attach a tamed pet to the player.  Pets that are attached to the player move with the player through walking, running, swimming, flying, etc.  Take your best friend everywhere!


## Playable behavior

- Craft the harness with `LLL / SLS / LLL` recipe (`L` = leather, `S` = string).
- Use the harness to attach a lead to your pet.
- After attaching the lead to your pet, place the harness into your legs slot and the pet will snap to you.
- Travel around with your pet (I personally like taking them diamond mining).
- To take the pet off the harness, unequip the harness from your leggings slot and disconnect the lead the way you normally would.

You can wear the harness without a pet in it for rizzy german drip.

## Requirements

- Minecraft Java 26.2
- Java 25
- Fabric Loader 0.19.3 or a compatible newer 26.2 loader
- Fabric API 0.158.0+26.2 or a compatible newer 26.2 build
- matching mod JAR on server and clients

## Build, test, and launch

```sh
./gradlew test
./gradlew runGameTest
./gradlew clean build
```

On Linux x86_64, `gradlew` can bootstrap a checksum-pinned Temurin 25 JDK into ignored `.jdk/`; otherwise set `JAVA_HOME` to Java 25. Artifacts appear under `build/libs/`.

Development launch tasks default to repository-local `run/client` and `run/server`:

```sh
./gradlew runClient
./gradlew runServer
```
