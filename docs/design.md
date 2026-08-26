# Design and lifecycle invariants

## State and identity

The single harness item has three logical states:

1. **WHOLE** — no link or captured payload.
2. **LINKED** — a versioned link containing link, owner, and adult-wolf UUIDs, mirrored by one live wolf.
3. **OCCUPIED** — the link, server-only entity NBT, and compact client render data. The represented live wolf must be absent.

Only the recorded owner may link, detach, or reset. Reciprocal identity requires all three UUIDs to match. Components reject unsupported schema versions. The item stacks to one, equips only in LEGS, grants no armor attributes, and keeps the same item model in every state.

Only a living, adult, unleashed, tamed wolf owned by the user is eligible for initial linking. Passenger and vehicle state is rejected when capture is attempted, not when the link is created. Other species, baby wolves, and cats are not supported.

```text
WHOLE -- use on eligible wolf --------------------> LINKED
LINKED -- use on exact reciprocal wolf ----------> WHOLE
LINKED -- owner sneak-uses in air ---------------> WHOLE
LINKED -- exact stack enters LEGS ----------------> OCCUPIED
OCCUPIED -- exact stack exits LEGS --------------> LINKED + restored wolf
OCCUPIED -- death with keepInventory ------------> OCCUPIED
OCCUPIED -- dropping death after safe restore ---> LINKED + normal drops
```

## Capture

Capture requires the actual LEGS stack, an exact reciprocal adult wolf that is neither passenger nor vehicle, and no other occupied representation of that pet. The server saves and validates the complete wolf NBT, writes the payload and render projection to the equipped stack, then reads the stack back and verifies the commit before discarding the live wolf. Any ambiguous result retains a recoverable representation and fails closed.

`CapturedPetData` has no network codec. Item packet encoding removes it before sending a stack to clients; clients receive only link identity and compact render data. Creative slot writes are rejected if they target an occupied server slot, include captured data, or conflict with occupied authority.

## Restore

Restore locates one exact occupied stack among online player equipment and inventories, open menus and cursors, and loaded item entities. Duplicate or conflicting representations prevent mutation. Closed containers and offline holders are not scanned; reconciliation resumes when the original stack becomes visible.

The server validates the payload, checks every loaded dimension for the UUID, finds a safe position near the recorded owner, and rebuilds the saved entity. UUID, owner, reciprocal link, age, tame/alive state, passenger state, collision, and world border are rechecked before insertion. Captured data is cleared only after successful insertion and exact UUID-index verification. Failures retain the payload and render data. An exact already-restored reciprocal wolf makes retries idempotent; any other UUID match is a conflict.

## Death and persistence

With `keepInventory`, the occupied stack remains occupied. For a dropping death, restore runs at the start of `ServerPlayer.die`; if safe restore fails, death is cancelled at one health to avoid losing or duplicating the wolf.

Persistence uses Minecraft's normal player inventory and entity chunk saves. The mod does not force saves or maintain a separate transaction log, so crash durability follows vanilla save ordering. World backups remain necessary.

## Rendering

The custom tether uses leash render state without changing vanilla leash state. The carried renderer reads only compact synchronized data. One native adult-wolf model serves all nine 26.2 variants by submitting the captured `wolf.getTexture()` identifier, with a separate collar and carried-only harness pass. Four independent damped leg channels use player motion. Baby geometry and a wolf-armor overlay are not implemented.

See [the renderer contract](../design/blockbench/renderer-contract.md) for hierarchy and texture rules.

## Recovery rules

- Never clear captured data after malformed input, identity conflict, unsafe placement, failed insertion, or ambiguous stack lookup.
- A whole harness may replace a stale link on an otherwise eligible owned wolf.
- Only the recorded owner may reset a stale linked item, and never while captured data exists.
- If the only occupied stack is destroyed rather than moved, no unverified copy may spawn a wolf; recovery requires the original stack to reappear.
