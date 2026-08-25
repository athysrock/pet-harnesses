# Development log

All commands ran in `/home/fuck/projects/pet-harnesses` on 2026-08-24. RED failures were expected missing-symbol failures unless noted.

## Toolchain/scaffold

- `env -u JAVA_HOME PATH=/usr/local/bin:/usr/bin:/bin ./gradlew test --no-daemon` — GREEN; wrapper used project-local Temurin 25.0.4.1, Loom 1.17.19, no tests yet.

## RED/GREEN evidence

1. Eligibility:
   - RED: `./gradlew test --tests '*PetEligibilityTest' --no-daemon` — failed compilation; eligibility API did not exist.
   - GREEN: same command — 1 test passed.
2. Attach/detach state:
   - RED: `./gradlew test --tests '*HarnessTransitionsTest' --no-daemon` — failed compilation; state transition API did not exist.
   - GREEN: same command — 2 tests passed.
3. Capture ordering and opaque payload preservation:
   - RED: `./gradlew test --tests '*HarnessTransactionsTest' --no-daemon` — failed compilation; capture port/transaction did not exist.
   - GREEN: same command — serialize -> commit -> remove tests passed.
4. Restore ordering and duplicate UUID:
   - RED: `./gradlew test --tests '*HarnessTransactionsTest' --no-daemon` — failed compilation; restore port did not exist.
   - GREEN: same command — 4 transaction tests passed, including insert-before-clear and duplicate fail-closed.
5. Death policy:
   - RED: `./gradlew test --tests '*DeathPolicyTest' --no-daemon` — failed compilation; death policy did not exist.
   - GREEN: same command — ordinary release and keepInventory retention passed.
6. Item/resource contract:
   - RED: `./gradlew test --tests '*ResourceAndItemContractTest' --no-daemon` — failed compilation; harness item did not exist.
   - Integration test exploration then exposed standalone Minecraft bootstrap/registry limitations; the test was narrowed to loader-independent registration constants plus exact resources. GREEN: 3 tests passed.
7. Complete captured NBT codec:
   - RED: `./gradlew test --tests '*CapturedPetDataCodecTest' --no-daemon` — failed compilation; codec did not exist.
   - GREEN: same command — arbitrary entity/equipment/mod NBT round-trip passed.
8. Replacement overflow:
   - RED: `./gradlew test --tests '*InventoryExitPolicyTest' --no-daemon` — failed compilation; destination policy did not exist.
   - GREEN: same command — replaced slot/inventory/world-drop cases passed.
9. Reciprocal link identity and stale-link recovery:
   - Added random per-harness link identity, exact detach matching, stale pet relink, owner-only item reset, and the OCCUPIED reset guard.
   - Focused transition/recovery tests cover copied old halves, mismatched owners/links, server-side decisions, and refusal to clear captured payloads.

## Review hardening cycles

1. First hardening cycle:
   - Tightened capture/restore durability semantics, retry behavior, UUID conflict handling, identity checks, stale-link recovery, and death dispositions.
   - Added reconciliation-required outcomes that retain OCCUPIED whenever a restore clear or capture rollback cannot be confirmed safely.
   - Full suite after this cycle: 28 tests passed.
2. Second independent-review hardening cycle:
   - RED: `./gradlew test --tests '*HarnessTransactionsTest' --no-daemon` — compilation failed because the explicit rollback outcome contract did not exist.
   - GREEN: same command — 10 transaction tests passed, including all four failed-removal rollback outcomes.
   - RED: `./gradlew test --tests '*HarnessStateIdentityTest.capturedPetRejectsFutureDomainSchema' --no-daemon` — failed because a future domain schema was accepted.
   - GREEN: same command — future domain schema rejection passed after requiring exactly `CapturedPet.CURRENT_SCHEMA`.
   - Full current suite: 31 tests passed.

Final full-suite/build evidence:

- `./gradlew clean build --no-daemon` — GREEN; 31 tests passed; JAR, sources JAR, check, and build completed.
- `./gradlew runServer --no-daemon` — GREEN in isolated `run/` on port 25566; Fabric loaded `pet_harnesses 0.1.0`, Minecraft reached `Done (1.939s)`, then accepted `stop` and exited cleanly. An initial attempt reached mod loading but found the default port 25565 already occupied; the isolated dev port was changed and the run repeated successfully.
- JSON parser validation — 9 JSON resources valid.
- `sh -n gradlew && sh -n scripts/bootstrap-jdk.sh` — GREEN.
