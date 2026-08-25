# Design

## Requirements and invariants

The harness has one item registry identity and three logical states:

1. **WHOLE** — no owner, pet, or captured payload.
2. **LINKED** — owner UUID, pet UUID, and random per-harness link UUID; a matching persistent attachment normally exists on one live tamed cat/wolf.
3. **OCCUPIED** — the same link plus a versioned complete entity NBT payload. The live pet must not exist in a reconciled state.

Only the pet's owner may create or recover a link. Detach requires exact per-harness link UUID, owner UUID, and pet UUID equality on both halves. Captured payload identity must equal harness link identity. The domain `CapturedPet` accepts exactly `CURRENT_SCHEMA`; unsupported earlier or future payload/schema versions are rejected. No armor attributes are added.

## State machine

```text
WHOLE -- owner uses on eligible cat/wolf ----------> LINKED (fresh link UUID)
LINKED -- owner uses on exact matching pet --------> WHOLE
LINKED -- owner sneak-uses in air, no payload -----> WHOLE
LINKED -- enters LEGS / capture transaction ------> OCCUPIED
OCCUPIED -- exits LEGS / restore transaction -----> LINKED
OCCUPIED -- death + keepInventory ----------------> OCCUPIED
OCCUPIED -- death + drops / successful restore ---> LINKED + dropped harness
```

Invalid identities and phases do not mutate state. WHOLE and LINKED death-policy inputs return `UNCHANGED`.

## Stale-link recovery

Recovery is deliberately limited while actual capture is not enabled:

- A whole harness used on an owned cat/wolf replaces any stale `pet_link` with a newly generated link UUID and writes that same identity to the item.
- A linked human half is reset by its recorded owner by **sneak-using it in air**. The server removes only the item's link; it does not mutate any pet.
- Item reset is refused whenever `captured_pet` exists, preventing recovery from discarding a captured pet.
- A stale/copied old human half cannot detach a newly linked pet because its link UUID differs. It can only be owner-reset as above.
- Pet interaction and every mutation are decided on the server. Client prediction returns without consulting unsynchronized `pet_link`.

A missing pet can therefore leave only a resettable human half; a missing item can leave only a pet link replaceable by its owner. World reload preserves both codecs. Game-level save/reload tests remain pending; the in-game boundary delegates its decisions to focused pure-policy tests.

## Capture transaction

1. Verify phase and reciprocal identity.
2. Serialize the complete entity, including UUID, owner, name, age, health, variants, collars, all equipment, and ordinary/modded persisted data.
3. Commit the versioned `captured_pet` component and verify OCCUPIED is durably readable.
4. Only then remove the live entity; `true` confirms durable matching-live absence, while `false` is explicitly ambiguous.
5. After a false removal result, attempt rollback only through `rollbackOccupiedIfMatchingLivePresent(occupied, linked)`. A safe rollback result requires a post-mutation durable read confirming **both** the exact LINKED item state and durable presence of the live pet matching the occupied payload.

Only `LINKED_AND_MATCHING_LIVE_CONFIRMED` may return an ordinary `REMOVAL_FAILED` with LINKED. If live presence is `LIVE_ABSENT` or `LIVE_PRESENCE_UNKNOWN`, or the LINKED mutation is not durable, the adapter must not discard the captured payload and the transaction returns `ROLLBACK_FAILED_RECONCILIATION_REQUIRED` with OCCUPIED. Callers must retain/quarantine that payload and reconcile by stable pet/link identity; the result must not be treated as ordinary OCCUPIED or LINKED.

`CapturePort` is deliberately asymmetric: a false `removeLive` never proves that the pet remains, and LINKED durability alone never proves rollback safety. `rollbackOccupiedIfMatchingLivePresent` must retain or restore OCCUPIED unless it can return the sole safe outcome after verifying both durable postconditions. Its outcomes, rather than a boolean, make absence, ambiguity, and mutation failure explicit and retry-safe.

## Restore transaction

1. Verify OCCUPIED and inspect the original UUID plus captured identity.
2. `CONFLICT` fails closed. `MATCHING_RESTORED` means an earlier insertion succeeded and skips reconstruction/insertion on retry.
3. For `ABSENT`, reconstruct only from the stored complete payload; never construct a generic cat/wolf fallback.
4. Place near the owner and require world insertion with the original UUID to succeed.
5. Only after insertion, clear captured data and verify LINKED is durably readable.

If insertion succeeds but clear verification fails, return `CLEAR_FAILED_RECONCILIATION_REQUIRED` and the original OCCUPIED state. This is **not normal OCCUPIED**: the matching live pet may already exist while a reusable payload remains. Quarantine the item; on retry the adapter must return `MATCHING_RESTORED`, skip another insertion, and retry only the idempotent clear. A different entity with that UUID is `CONFLICT` and must never be cleared automatically.

All `RestorePort` mutations return `true` only after their durable postcondition is readable, including already-achieved retry states. This is the exact recovery contract for interruption and duplicate-risk windows.

## Death

With keepInventory, no restore call occurs and OCCUPIED survives respawn. For ordinary dropping death, restore must succeed before a non-occupied LINKED harness may drop. A UUID conflict, insertion failure, or clear failure retains the reported reconciliation state instead of falsely claiming a safe drop.

## Implemented Fabric boundary

- True registered item, max stack one, vanilla `EQUIPPABLE` LEGS component, no attribute modifiers.
- Shaped recipe, English name, three original placeholder item models/textures.
- Persistent item `harness_link` component and entity `pet_link` attachment.
- Server-authoritative owner/species link, exact-identity detach, stale-pet relink, and owner-only stale-item reset.
- Persistent versioned `captured_pet` complete-NBT component schema.

## Pending exact integrations

The exact 26.2 event path must prove all slot mutations and death ordering before being enabled. Pending work:

- equipment-change adapter for manual equip, shift-click, replacement, commands, and inventory mods;
- complete `TagValueOutput` save and `EntityType` reconstruction adapter with safe nearby placement;
- durable adapter implementation and reconciliation/quarantine UI for represented interruption states;
- player death drop interception versus keepInventory respawn;
- logout/login, dimension, corrupted payload, and dedicated-server GameTests;
- custom client tether and carried models/animations.

The current mod does **not** fake these with inventory polling, vanilla leash state, a passenger, or a hidden pet.
