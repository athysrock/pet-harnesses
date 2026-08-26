package io.github.athysrock.petharnesses.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.athysrock.petharnesses.physics.CarriedWolfPhysics;
import io.github.athysrock.petharnesses.physics.LegAngles;
import io.github.athysrock.petharnesses.physics.MotionInput;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class CarriedWolfRenderPhysicsTest {
    private static final double EPSILON = 1.0e-12;
    private static final MotionInput RESTING = MotionInput.resting();
    private static final MotionInput JUMPING =
            new MotionInput(0.35, 0.42, 0.42, 0.0, 0.2, false, false);
    private static final MotionInput TURNING =
            new MotionInput(0.8, -0.1, -0.3, 0.7, -0.25, true, false);

    @Test
    void exactlyTwentyTickGapIntegratesEveryFixedStep() {
        UUID petId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        CarriedWolfRenderPhysics renderPhysics = new CarriedWolfRenderPhysics();
        renderPhysics.sample(7, petId, 100L, 0.0, RESTING);

        CarriedWolfPhysics expected = new CarriedWolfPhysics();
        expected.resetToTarget(RESTING);
        for (int tick = 0; tick < 20; tick++) {
            expected.step(TURNING, CarriedWolfRenderPhysics.FIXED_STEP_SECONDS);
        }
        assertNotEquals(CarriedWolfPhysics.targets(TURNING).rightFront(),
                expected.current().rightFront(), 1.0e-6,
                "the test input must distinguish fixed-step integration from a reset");

        LegAngles actual = renderPhysics.sample(7, petId, 120L, 1.0, TURNING);

        assertAngles(expected.current(), actual);
    }

    @Test
    void longStallResetsToCurrentTargetWithoutCarryingStaleElapsed() {
        UUID petId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        CarriedWolfRenderPhysics renderPhysics = new CarriedWolfRenderPhysics();
        renderPhysics.sample(8, petId, 200L, 0.0, RESTING);

        LegAngles afterStall = renderPhysics.sample(
                8, petId, 221L, 0.37, JUMPING);
        assertAngles(CarriedWolfPhysics.targets(JUMPING), afterStall);

        CarriedWolfPhysics expectedNextTick = new CarriedWolfPhysics();
        expectedNextTick.resetToTarget(JUMPING);
        expectedNextTick.step(TURNING, CarriedWolfRenderPhysics.FIXED_STEP_SECONDS);

        LegAngles nextTick = renderPhysics.sample(8, petId, 222L, 1.0, TURNING);
        assertAngles(expectedNextTick.current(), nextTick);
    }

    @Test
    void productionSampleCallEvictsEntriesByAgeAndHardBound() {
        CarriedWolfRenderPhysics renderPhysics = new CarriedWolfRenderPhysics();
        renderPhysics.sample(1, petId(1), 10L, 0.0, RESTING);

        renderPhysics.sample(
                2, petId(2), 10L + CarriedWolfRenderPhysics.STALE_ENTRY_TICKS + 1L,
                0.0, RESTING);

        assertFalse(renderPhysics.hasEntry(1), "an entry older than the production TTL must be evicted");
        assertTrue(renderPhysics.hasEntry(2));

        long sameTick = 10L + CarriedWolfRenderPhysics.STALE_ENTRY_TICKS + 1L;
        for (int id = 3; id <= CarriedWolfRenderPhysics.MAX_ENTRIES + 2; id++) {
            renderPhysics.sample(id, petId(id), sameTick, 0.0, RESTING);
        }

        assertEquals(CarriedWolfRenderPhysics.MAX_ENTRIES, renderPhysics.entryCount());
        assertFalse(renderPhysics.hasEntry(2), "the least-recently-seen entry must be evicted at the cap");
        assertTrue(renderPhysics.hasEntry(CarriedWolfRenderPhysics.MAX_ENTRIES + 2));
    }


    private static UUID petId(int id) {
        return new UUID(0L, id);
    }

    private static void assertAngles(LegAngles expected, LegAngles actual) {
        assertEquals(expected.rightFront(), actual.rightFront(), EPSILON);
        assertEquals(expected.leftFront(), actual.leftFront(), EPSILON);
        assertEquals(expected.rightHind(), actual.rightHind(), EPSILON);
        assertEquals(expected.leftHind(), actual.leftHind(), EPSILON);
    }
}
