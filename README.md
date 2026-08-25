# Athysrock's Pet Harnesses

Fabric mod scaffold for Minecraft Java **26.2**. The mod registers `pet_harnesses:pet_harness`, a non-stacking, zero-protection item that equips in the legs slot.

## Current behavior

- Craft with exactly `LLL / SLS / LLL` (`L` = leather, `S` = string).
- Right-click your own tamed cat or wolf with a whole harness to link it. Untamed pets, another player's pets, and unsupported entities are rejected server-side.
- Right-click the same pet with its matching linked human half to detach and return the whole harness.
- If an owned pet retains a stale link because its former item disappeared, right-click it with a **whole** harness. The server replaces the stale pet link with a fresh, random per-harness link identity.
- To recover a linked item whose pet is gone, **sneak-use the item in air**. Only the recorded owner may reset it, and reset is refused if the item has a `captured_pet` payload.
- A copied or formerly duplicated old linked item cannot detach a newly re-linked pet: exact per-harness link identity must match. It remains a harmless stale human half and its recorded owner may reset it in air.
- Persistent codec-backed item/entity link identity is implemented.
- Whole, linked/human-half, and occupied item states have intentionally simple original placeholder icons/models.
- A versioned complete-entity-NBT item component and tested, failure-reporting capture/restore/death policy seams are present.

All recovery mutations are server-authoritative. Client prediction does not read the persistent `pet_link` attachment because that attachment is intentionally not synchronized.

**Custom carried cat/dog models and animations are not implemented yet.** The visual tether is also pending; this scaffold deliberately does not use vanilla leash state or a brittle fake.

The Fabric wiring for detecting every legs-slot mutation, serializing/removing the live pet, restoring it after every possible unequip/replacement, and death-drop interception remains pending. The domain transactions are executable and tested, but no incomplete lifecycle hook is enabled in gameplay; therefore the current build links/unlinks and recovers stale links but does not capture pets on equip yet. See [docs/design.md](docs/design.md).

## Requirements

Players and the server need matching versions of:

- Minecraft Java 26.2
- Java 25
- Fabric Loader 0.19.3 or a compatible newer 26.2 loader
- Fabric API 0.158.0+26.2 or a compatible newer 26.2 build
- this mod JAR

Do not install an unfinished development build into an irreplaceable world. Test with a disposable world and backups first.

## Build and test

```sh
./gradlew test
./gradlew clean build
```

On Linux x86_64, `gradlew` automatically downloads a checksum-pinned Temurin 25 JDK into ignored `.jdk/` when no Java is installed. The bootstrap validates Java 25 and extracts into a temporary directory before renaming it into place. Otherwise set `JAVA_HOME` to a Java 25 JDK. The bootstrap makes no system-wide changes.

Artifacts are written to `build/libs/`. The installable artifact is `pet-harnesses-<version>.jar`; the `-sources.jar` is not a mod to install. Run a disposable development client or server with:

```sh
./gradlew runClient
./gradlew runServer
```

For a development server, accept Mojang's EULA only in the generated ignored `run/eula.txt`. Never point Loom at the production server or world.

## Install

Copy Fabric API and the built `pet-harnesses-<version>.jar` into both client and server `mods/` directories. A server-only install is not supported because item resources must be available to clients.
