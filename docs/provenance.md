# Provenance

Research was re-resolved against these exact upstream revisions on 2026-08-24:

- **Carry On** (LGPL-3.0), repository <https://github.com/Tschipp/CarryOn>, commit [`e50ddbc1c7461f381c62af5f4960db9d97751d16`](https://github.com/Tschipp/CarryOn/tree/e50ddbc1c7461f381c62af5f4960db9d97751d16). Reviewed `Common/src/main/java/tschipp/carryon/client/render/CarriedObjectRender.java` for player-relative entity-rendering feasibility and the repository license/build structure.
- **Proper Pet Teleport** (LGPL-3.0-or-later), repository <https://github.com/TheEpicBlock/PPeTP>, commit [`d0618d017e7d6406aea1ef1f4ce4c4b30f6bf57b`](https://github.com/TheEpicBlock/PPeTP/tree/d0618d017e7d6406aea1ef1f4ce4c4b30f6bf57b). Reviewed `src/main/java/nl/theepicblock/ppetp/PetTeleporter.java`, `src/main/java/nl/theepicblock/ppetp/PlayerPetStorage.java`, and `src/gametest/java/nl/theepicblock/gametest/BasicGameTest.java` for save-before-discard, reconstruction, and integration-test concepts.
- **Eternally Bound** (MIT), repository <https://codeberg.org/enjarai/eternally-bound>, commit [`997d95b3fbcb67ce20404b5345ab93298ebdadd5`](https://codeberg.org/enjarai/eternally-bound/commit/997d95b3fbcb67ce20404b5345ab93298ebdadd5). Reviewed `src/main/java/dev/enjarai/collared/item/CollarItem.java`, `src/main/java/dev/enjarai/collared/item/LeashItem.java`, and `src/main/java/dev/enjarai/collared/renderer/LeashRenderer.java` for component-backed link identity and custom tether feasibility.

The three 16×16 placeholder PNG textures under `src/main/resources/assets/pet_harnesses/textures/item/` are original project placeholders created for this scaffold; they are not copied or adapted from upstream assets. The implementation, models, recipes, and documentation are likewise clean-room project work: concepts were studied, but no upstream source snippets or assets were copied.

Carry Cats and Perfected Pet are all-rights-reserved and were not decompiled or copied. If future work adapts source rather than concepts, preserve the upstream license notice and record the exact file and revision here.
