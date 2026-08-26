# Blockbench reference assets

These `.bbmodel` files are editable geometry references for the native `ModelPart` renderer. They contain no animations and do not require GeckoLib.

## Projects

- `minecraft_26_2_pale_wolf_carry_reference.bbmodel` is the canonical adult-wolf carry project.
- `minecraft_26_2_<variant>_wolf_carry_reference.bbmodel` provides the same project for `ashen`, `black`, `chestnut`, `pale`, `rusty`, `snowy`, `spotted`, `striped`, and `woods`. Only project identity and the embedded wild-wolf texture differ.
- `player_short_lederhosen_harness.bbmodel` contains the player harness and a locked Steve-scale guide.

The wolf projects embed Mojang wolf textures for editing. Those texture pixels remain Mojang content and are not covered by this project's MIT license. Runtime rendering uses Minecraft's built-in variant texture identifiers and does not ship duplicate wolf texture PNGs. `steve_reference_palette.png` is an original flat-color guide.

Open a project with Blockbench, for example:

```sh
blockbench design/blockbench/minecraft_26_2_pale_wolf_carry_reference.bbmodel
blockbench design/blockbench/player_short_lederhosen_harness.bbmodel
```

## Wolf model contract

- Player front is negative Z. The locked Steve guide is 28.8 model units (1.8 blocks) tall.
- `carried_wolf_front_mount_root` is the carry root.
- Apply one uniform runtime scale at `carried_adult_wolf_RUNTIME_SCALE_ROOT__1_0_CANONICAL`. Its saved value is `1.0`; do not scale individual cubes or scale again in child or leg code.
- `carry_pose_reference_NEUTRAL_UPRIGHT` contains the locked vanilla source geometry and the carried-only harness as siblings.
- Preserve all source cubes, UVs, pivots, transforms, and the four independent leg groups under `reference_geometry_VANILLA_26_2_SOURCE_LOCKED`.
- Keep `carried_wolf_SQUARE_I_HARNESS__ORIGINAL__CARRIED_ONLY` outside every source and leg group. Its front is a short upper bar, longer lower bar, centered connector, side wraps, and four brass joins.
- Keep the projects free of animations and keyframes.

The canonical source records are stored in `baselines/minecraft_26_2_pale_wolf_source_baseline.json` for maintainers comparing geometry, UVs, pivots, and transforms.

## Player model contract

- Parent `PLAYER_HARNESS_TORSO_STRAPS__NO_ARM_ATTACHMENTS` to the player body.
- Parent each short/cuff group to its matching leg.
- Never attach torso straps or hardware to either arm; this preserves standard and slim skin compatibility.

See [renderer-contract.md](renderer-contract.md) for runtime integration details. The files under `verification/` are visual references only.
