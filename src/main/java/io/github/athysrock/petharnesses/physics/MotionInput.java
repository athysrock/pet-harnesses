package io.github.athysrock.petharnesses.physics;

/** Player-local motion sampled once per client tick; no limb state is networked. */
public record MotionInput(
        double horizontalSpeed,
        double verticalVelocity,
        double verticalAcceleration,
        double yawRate,
        double pitchRadians,
        boolean onGround,
        boolean fallFlying) {
    public static MotionInput resting() {
        return new MotionInput(0.0, 0.0, 0.0, 0.0, 0.0, true, false);
    }
}
