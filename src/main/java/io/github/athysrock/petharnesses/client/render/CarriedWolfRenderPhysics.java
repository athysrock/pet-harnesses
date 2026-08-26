package io.github.athysrock.petharnesses.client.render;

import io.github.athysrock.petharnesses.physics.CarriedWolfPhysics;
import io.github.athysrock.petharnesses.physics.LegAngles;
import io.github.athysrock.petharnesses.physics.MotionInput;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Owns fixed-tick render physics, culling recovery, and bounded per-player controller state. */
final class CarriedWolfRenderPhysics {
    static final double FIXED_STEP_SECONDS = 1.0 / 20.0;
    static final long MAX_INTEGRATED_GAP_TICKS = 20L;
    static final long STALE_ENTRY_TICKS = 20L * 60L;
    static final int MAX_ENTRIES = 256;

    private final LinkedHashMap<Integer, PhysicsEntry> entries =
            new LinkedHashMap<>(16, 0.75F, true);

    LegAngles sample(
            int playerEntityId,
            UUID petId,
            long gameTick,
            double partialTicks,
            MotionInput input) {
        evictStale(gameTick);

        PhysicsEntry entry = entries.get(playerEntityId);
        if (entry == null || !entry.petId.equals(petId)) {
            entry = new PhysicsEntry(petId, gameTick, input);
            entries.put(playerEntityId, entry);
        } else {
            long elapsedTicks = gameTick - entry.lastTick;
            if (elapsedTicks < 0L || elapsedTicks > MAX_INTEGRATED_GAP_TICKS) {
                entry.controller.resetToTarget(input);
            } else {
                for (long tick = 0L; tick < elapsedTicks; tick++) {
                    entry.controller.step(input, FIXED_STEP_SECONDS);
                }
            }
            if (elapsedTicks != 0L) entry.lastTick = gameTick;
            entry.lastSeenTick = gameTick;
        }

        evictOverCapacity();
        return entry.controller.sample(clampPartialTick(partialTicks));
    }

    int entryCount() {
        return entries.size();
    }

    boolean hasEntry(int playerEntityId) {
        return entries.containsKey(playerEntityId);
    }

    private void evictStale(long gameTick) {
        entries.entrySet().removeIf(entry ->
                gameTick > entry.getValue().lastSeenTick
                        && gameTick - entry.getValue().lastSeenTick > STALE_ENTRY_TICKS);
    }

    private void evictOverCapacity() {
        Iterator<Map.Entry<Integer, PhysicsEntry>> oldest = entries.entrySet().iterator();
        while (entries.size() > MAX_ENTRIES && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
    }

    private static double clampPartialTick(double partialTicks) {
        if (!Double.isFinite(partialTicks)) return 0.0;
        return Math.max(0.0, Math.min(1.0, partialTicks));
    }

    private static final class PhysicsEntry {
        private final UUID petId;
        private final CarriedWolfPhysics controller = new CarriedWolfPhysics();
        private long lastTick;
        private long lastSeenTick;

        private PhysicsEntry(UUID petId, long gameTick, MotionInput input) {
            this.petId = petId;
            this.lastTick = gameTick;
            this.lastSeenTick = gameTick;
            controller.resetToTarget(input);
        }
    }
}
