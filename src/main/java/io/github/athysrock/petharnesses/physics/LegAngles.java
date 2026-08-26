package io.github.athysrock.petharnesses.physics;

public record LegAngles(double rightFront, double leftFront, double rightHind, double leftHind) {
    public LegAngles lerp(LegAngles other, double amount) {
        double t = clamp(amount, 0.0, 1.0);
        return new LegAngles(
                rightFront + (other.rightFront - rightFront) * t,
                leftFront + (other.leftFront - leftFront) * t,
                rightHind + (other.rightHind - rightHind) * t,
                leftHind + (other.leftHind - leftHind) * t);
    }

    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : min;
    }
}
