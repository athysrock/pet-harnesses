package io.github.athysrock.petharnesses.physics;

/** Deterministic four-spring carry motion with exact authored endpoint clamps. */
public final class CarriedWolfPhysics {
    public static final double FRONT_MIN_REST = Math.PI / 8.0;
    public static final double FRONT_MAX_SWING = 25.0 * Math.PI / 72.0;
    public static final double HIND_MIN_REST = Math.PI / 6.0;
    public static final double HIND_MAX_SWING = Math.PI / 3.0;
    private static final double MAX_SUBSTEP = 1.0 / 120.0;
    private static final double STIFFNESS = 44.0;
    private static final double DAMPING = 2.4;

    private final Spring rightFront = new Spring(FRONT_MIN_REST);
    private final Spring leftFront = new Spring(FRONT_MIN_REST);
    private final Spring rightHind = new Spring(HIND_MIN_REST);
    private final Spring leftHind = new Spring(HIND_MIN_REST);
    private LegAngles previous = current();

    public static LegAngles targets(MotionInput raw) {
        MotionInput input = sanitized(raw);
        if (!input.fallFlying() && !input.onGround()
                && (input.verticalVelocity() >= 0.08 || input.verticalAcceleration() >= 0.08)) {
            return new LegAngles(FRONT_MAX_SWING, FRONT_MAX_SWING, HIND_MAX_SWING, HIND_MAX_SWING);
        }

        double frontRange = FRONT_MAX_SWING - FRONT_MIN_REST;
        double hindRange = HIND_MAX_SWING - HIND_MIN_REST;
        double frontBase;
        double hindBase;
        if (input.fallFlying()) {
            double speedProgress = smoothStep(clamp((input.horizontalSpeed() - 0.2) / 1.4, 0.0, 1.0));
            frontBase = lerp(FRONT_MAX_SWING, FRONT_MIN_REST, speedProgress);
            hindBase = lerp(HIND_MAX_SWING, HIND_MIN_REST, speedProgress);
        } else {
            double speed = clamp(input.horizontalSpeed() / 1.4, 0.0, 1.0);
            frontBase = FRONT_MIN_REST + frontRange * speed * 0.08;
            hindBase = HIND_MIN_REST + hindRange * speed * 0.05;
        }

        double yaw = clamp(input.yawRate() / 2.0, -1.0, 1.0);
        double pitch = clamp(input.pitchRadians() / (Math.PI / 2.0), -1.0, 1.0);
        double vertical = clamp(-input.verticalAcceleration(), -1.0, 1.0);
        double rightFrontTarget = frontBase + frontRange * (0.25 * yaw + 0.14 * pitch + 0.28 * vertical);
        double leftFrontTarget = frontBase + frontRange * (-0.25 * yaw + 0.08 * pitch + 0.18 * vertical);
        double rightHindTarget = hindBase + hindRange * (-0.18 * yaw - 0.10 * pitch - 0.14 * vertical);
        double leftHindTarget = hindBase + hindRange * (0.18 * yaw - 0.06 * pitch - 0.08 * vertical);
        return new LegAngles(
                clamp(rightFrontTarget, FRONT_MIN_REST, FRONT_MAX_SWING),
                clamp(leftFrontTarget, FRONT_MIN_REST, FRONT_MAX_SWING),
                clamp(rightHindTarget, HIND_MIN_REST, HIND_MAX_SWING),
                clamp(leftHindTarget, HIND_MIN_REST, HIND_MAX_SWING));
    }

    public void step(MotionInput input, double elapsedSeconds) {
        previous = current();
        double dt = Double.isFinite(elapsedSeconds) && elapsedSeconds > 0.0
                ? Math.min(elapsedSeconds, 0.25)
                : 1.0 / 20.0;
        int steps = Math.max(1, (int) Math.ceil(dt / MAX_SUBSTEP));
        double substep = dt / steps;
        LegAngles target = targets(input);
        for (int i = 0; i < steps; i++) {
            rightFront.advance(target.rightFront(), substep, FRONT_MIN_REST, FRONT_MAX_SWING);
            leftFront.advance(target.leftFront(), substep, FRONT_MIN_REST, FRONT_MAX_SWING);
            rightHind.advance(target.rightHind(), substep, HIND_MIN_REST, HIND_MAX_SWING);
            leftHind.advance(target.leftHind(), substep, HIND_MIN_REST, HIND_MAX_SWING);
        }
    }

    /** Clears accumulated velocity and makes both interpolation endpoints the current target. */
    public void resetToTarget(MotionInput input) {
        LegAngles target = targets(input);
        rightFront.reset(target.rightFront());
        leftFront.reset(target.leftFront());
        rightHind.reset(target.rightHind());
        leftHind.reset(target.leftHind());
        previous = target;
    }

    public LegAngles current() {
        return new LegAngles(rightFront.angle, leftFront.angle, rightHind.angle, leftHind.angle);
    }

    public LegAngles sample(double partialTicks) {
        return previous.lerp(current(), partialTicks);
    }

    private static MotionInput sanitized(MotionInput input) {
        if (input == null) return MotionInput.resting();
        return new MotionInput(
                finite(input.horizontalSpeed()),
                finite(input.verticalVelocity()),
                finite(input.verticalAcceleration()),
                finite(input.yawRate()),
                finite(input.pitchRadians()),
                input.onGround(),
                input.fallFlying());
    }

    private static double finite(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    private static double smoothStep(double value) {
        return value * value * (3.0 - 2.0 * value);
    }

    private static final class Spring {
        private double angle;
        private double velocity;

        private Spring(double angle) {
            this.angle = angle;
        }

        private void advance(double target, double dt, double min, double max) {
            double acceleration = STIFFNESS * (target - angle) - DAMPING * velocity;
            velocity += acceleration * dt;
            angle += velocity * dt;
            if (!Double.isFinite(angle) || !Double.isFinite(velocity)) {
                angle = clamp(target, min, max);
                velocity = 0.0;
            } else if (angle <= min) {
                angle = min;
                if (velocity < 0.0) velocity = 0.0;
            } else if (angle >= max) {
                angle = max;
                if (velocity > 0.0) velocity = 0.0;
            }
        }

        private void reset(double target) {
            angle = target;
            velocity = 0.0;
        }
    }
}
