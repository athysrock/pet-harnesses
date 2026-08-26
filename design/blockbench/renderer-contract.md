# Renderer contract

## Player harness

- Source: `player_short_lederhosen_harness.bbmodel`.
- Texture: `assets/pet_harnesses/textures/entity/lederhosen_harness.png`.
- Bake a dedicated native `ModelPart` layer.
- Parent torso straps, waistband, bib, suspenders, and hardware to the player body. Parent each short/cuff group to its matching leg. Never parent harness geometry to an arm.
- Render it only when a Pet Harness occupies LEGS. It is cosmetic and adds no armor attributes.

## Carried wolf

- Canonical source: `minecraft_26_2_pale_wolf_carry_reference.bbmodel`; the other `minecraft_26_2_<variant>_wolf_carry_reference.bbmodel` files differ only in identity and embedded editing texture.
- Harness texture: `assets/pet_harnesses/textures/entity/carried_wolf_square_i_harness.png`.
- `CarriedWolfModel.harness_root` remains a sibling of `source_root` under `carry_pose`, below the single `runtime_scale_root`.
- Preserve source geometry, UVs, transforms, and four independent leg pivots. Harness cubes must not be children of source or leg parts.
- Render the brown square-I harness only in the occupied `CarriedWolfRenderLayer`, using a separate cutout submission from the wolf and collar.
- Submit the `PetRenderData.texture()` identifier captured from `wolf.getTexture()`; do not add a variant allowlist or per-variant Java model.

## Live wolf

`WolfRendererMixin` projects only the custom tether into `WolfRenderState.leashStates`. It must not bake, attach, texture, or submit either harness model.

## Item

WHOLE, LINKED, and OCCUPIED states all use `pet_harnesses:pet_harness` and the same original 16×16 item sprite. Do not add state-specific item visuals without changing this contract.

## Visual tuning

Keep the hierarchy and palette fixed while checking adult carry scale, strap and collar clipping, crouch clipping, extreme leg swing, and elytra poses in third person.
