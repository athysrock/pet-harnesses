package io.github.athysrock.petharnesses.physics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class CarriedWolfPhysicsTest {
    private static final double EPSILON = 1.0e-6;

    @Test
    void exactRestJumpAndFlightSpeedEndpoints() {
        LegAngles rest = CarriedWolfPhysics.targets(MotionInput.resting());
        assertEquals(CarriedWolfPhysics.FRONT_MIN_REST, rest.rightFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.FRONT_MIN_REST, rest.leftFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MIN_REST, rest.rightHind(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MIN_REST, rest.leftHind(), EPSILON);

        LegAngles jump = CarriedWolfPhysics.targets(new MotionInput(0.0, 0.42, 0.42, 0.0, 0.0, false, false));
        assertEquals(CarriedWolfPhysics.FRONT_MAX_SWING, jump.rightFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.FRONT_MAX_SWING, jump.leftFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MAX_SWING, jump.rightHind(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MAX_SWING, jump.leftHind(), EPSILON);

        LegAngles slowFlight = CarriedWolfPhysics.targets(new MotionInput(0.1, -0.02, 0.0, 0.0, 0.0, false, true));
        LegAngles fastFlight = CarriedWolfPhysics.targets(new MotionInput(2.0, -0.02, 0.0, 0.0, 0.0, false, true));
        assertEquals(CarriedWolfPhysics.FRONT_MAX_SWING, slowFlight.rightFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.FRONT_MIN_REST, fastFlight.rightFront(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MAX_SWING, slowFlight.rightHind(), EPSILON);
        assertEquals(CarriedWolfPhysics.HIND_MIN_REST, fastFlight.rightHind(), EPSILON);
    }

    @Test
    void increasingFlightSpeedReturnsGraduallyAndMonotonicallyTowardRest() {
        double previousFront = Double.POSITIVE_INFINITY;
        double previousHind = Double.POSITIVE_INFINITY;
        for (double speed : new double[] {0.1, 0.3, 0.6, 0.9, 1.2, 1.6, 2.0}) {
            LegAngles target = CarriedWolfPhysics.targets(new MotionInput(speed, -0.05, 0.0, 0.0, 0.0, false, true));
            assertTrue(target.rightFront() <= previousFront + EPSILON);
            assertTrue(target.rightHind() <= previousHind + EPSILON);
            previousFront = target.rightFront();
            previousHind = target.rightHind();
        }
    }

    @Test
    void orientationAndVerticalAccelerationDriveAllFourLegsIndependently() {
        LegAngles target = CarriedWolfPhysics.targets(
                new MotionInput(0.55, -0.1, -0.25, 1.2, 0.35, true, false));
        assertNotEquals(target.rightFront(), target.leftFront());
        assertNotEquals(target.rightHind(), target.leftHind());
        assertNotEquals(target.rightFront(), target.rightHind());
        assertNotEquals(target.leftFront(), target.leftHind());
    }

    @Test
    void dampedSpringIsFiniteClampedAndFrameRateIndependent() {
        MotionInput jump = new MotionInput(0.0, 0.42, 0.42, 0.0, 0.0, false, false);
        CarriedWolfPhysics atTwentyFps = new CarriedWolfPhysics();
        CarriedWolfPhysics atOneTwentyFps = new CarriedWolfPhysics();
        for (int i = 0; i < 40; i++) atTwentyFps.step(jump, 1.0 / 20.0);
        for (int i = 0; i < 240; i++) atOneTwentyFps.step(jump, 1.0 / 120.0);
        assertAnglesNear(atTwentyFps.current(), atOneTwentyFps.current(), 0.003);

        MotionInput invalid = new MotionInput(
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.NaN, Double.POSITIVE_INFINITY, false, true);
        atTwentyFps.step(invalid, Double.POSITIVE_INFINITY);
        assertFiniteAndClamped(atTwentyFps.current());
    }

    @Test
    void jumpPulseSwingsLegsUpAndBackWithinThePlayersAirTime() {
        MotionInput takeoff = new MotionInput(0.0, 0.42, 0.42, 0.0, 0.0, false, false);
        CarriedWolfPhysics physics = new CarriedWolfPhysics();
        double frontRange = CarriedWolfPhysics.FRONT_MAX_SWING - CarriedWolfPhysics.FRONT_MIN_REST;

        stepFor(physics, takeoff, 0.25);
        double takeoffProgress = (physics.current().rightFront() - CarriedWolfPhysics.FRONT_MIN_REST) / frontRange;
        assertTrue(takeoffProgress > 0.75,
                "legs should complete most of their upward swing during the five-tick ascent");

        physics.step(MotionInput.resting(), 1.0 / 20.0);
        double freeSwingProgress = (physics.current().rightFront() - CarriedWolfPhysics.FRONT_MIN_REST) / frontRange;
        assertTrue(freeSwingProgress > takeoffProgress,
                "low damping should let upward momentum briefly carry the leg past the reversed target");

        stepFor(physics, MotionInput.resting(), 0.20);
        double landingProgress = (physics.current().rightFront() - CarriedWolfPhysics.FRONT_MIN_REST) / frontRange;
        assertTrue(landingProgress < 0.20,
                "legs should swing back down before the short jump has visibly finished");
    }

    @Test
    void looseSpringQuicklyOvershootsAndKeepsOscillatingAroundAnInteriorTarget() {
        MotionInput midFlight = new MotionInput(0.9, -0.05, 0.0, 0.0, 0.0, false, true);
        double target = CarriedWolfPhysics.targets(midFlight).rightFront();
        CarriedWolfPhysics physics = new CarriedWolfPhysics();

        stepFor(physics, midFlight, 0.45);
        assertTrue(physics.current().rightFront() > target + 0.08,
                "the floppy leg should visibly overshoot its target");

        stepFor(physics, midFlight, 0.50);
        assertTrue(physics.current().rightFront() < target - 0.015,
                "the broad oscillation should reverse visibly within one second");
    }

    @Test
    void flyingYawProducesVisibleAsymmetryAndRespondsWithinAQuickDirectionChange() {
        MotionInput neutral = new MotionInput(0.9, -0.08, 0.0, 0.0, 0.0, false, true);
        MotionInput turnRight = new MotionInput(0.9, -0.08, 0.0, 1.2, 0.0, false, true);
        MotionInput turnLeft = new MotionInput(0.9, -0.08, 0.0, -1.2, 0.0, false, true);
        CarriedWolfPhysics physics = new CarriedWolfPhysics();
        physics.resetToTarget(neutral);

        LegAngles rightTurnTarget = CarriedWolfPhysics.targets(turnRight);
        assertTrue(rightTurnTarget.rightFront() - rightTurnTarget.leftFront() > 0.20);
        assertTrue(rightTurnTarget.leftHind() - rightTurnTarget.rightHind() > 0.10);

        stepFor(physics, turnRight, 0.25);
        stepFor(physics, turnLeft, 0.25);
        assertTrue(physics.current().rightFront() - physics.current().leftFront() < -0.03,
                "loose legs should retain asymmetry while following a rapid yaw reversal");
    }

    @Test
    void stallsAndTeleportScaleInputsRemainFiniteAndAnatomicallyClamped() {
        CarriedWolfPhysics physics = new CarriedWolfPhysics();
        MotionInput teleport = new MotionInput(
                Double.MAX_VALUE, -Double.MAX_VALUE, Double.MAX_VALUE,
                Double.MAX_VALUE, -Double.MAX_VALUE, false, true);
        for (int i = 0; i < 20; i++) {
            physics.step(teleport, 1_000.0);
            assertFiniteAndClamped(physics.current());
        }
    }

    @Test
    void exactRestHasNoSelfExcitedJitter() {
        CarriedWolfPhysics physics = new CarriedWolfPhysics();
        LegAngles rest = physics.current();
        for (int i = 0; i < 400; i++) physics.step(MotionInput.resting(), 1.0 / 20.0);
        assertEquals(rest, physics.current());
        assertEquals(rest, physics.sample(0.5));
    }

    @Test
    void partialTickSamplingInterpolatesPreviousAndCurrentState() {
        CarriedWolfPhysics physics = new CarriedWolfPhysics();
        LegAngles before = physics.current();
        physics.step(new MotionInput(0.0, 0.42, 0.42, 0.0, 0.0, false, false), 0.05);
        LegAngles after = physics.current();
        assertEquals(before, physics.sample(0.0));
        assertEquals(after, physics.sample(1.0));
        LegAngles halfway = physics.sample(0.5);
        assertEquals((before.rightFront() + after.rightFront()) * 0.5, halfway.rightFront(), EPSILON);
    }

    private static void stepFor(CarriedWolfPhysics physics, MotionInput input, double seconds) {
        int ticks = (int) Math.round(seconds * 20.0);
        for (int i = 0; i < ticks; i++) physics.step(input, 1.0 / 20.0);
    }

    private static void assertFiniteAndClamped(LegAngles angles) {
        assertTrue(Double.isFinite(angles.rightFront()));
        assertTrue(Double.isFinite(angles.leftFront()));
        assertTrue(Double.isFinite(angles.rightHind()));
        assertTrue(Double.isFinite(angles.leftHind()));
        assertTrue(angles.rightFront() >= CarriedWolfPhysics.FRONT_MIN_REST);
        assertTrue(angles.rightFront() <= CarriedWolfPhysics.FRONT_MAX_SWING);
        assertTrue(angles.leftFront() >= CarriedWolfPhysics.FRONT_MIN_REST);
        assertTrue(angles.leftFront() <= CarriedWolfPhysics.FRONT_MAX_SWING);
        assertTrue(angles.rightHind() >= CarriedWolfPhysics.HIND_MIN_REST);
        assertTrue(angles.rightHind() <= CarriedWolfPhysics.HIND_MAX_SWING);
        assertTrue(angles.leftHind() >= CarriedWolfPhysics.HIND_MIN_REST);
        assertTrue(angles.leftHind() <= CarriedWolfPhysics.HIND_MAX_SWING);
    }

    private static void assertAnglesNear(LegAngles a, LegAngles b, double tolerance) {
        assertEquals(a.rightFront(), b.rightFront(), tolerance);
        assertEquals(a.leftFront(), b.leftFront(), tolerance);
        assertEquals(a.rightHind(), b.rightHind(), tolerance);
        assertEquals(a.leftHind(), b.leftHind(), tolerance);
    }
}
