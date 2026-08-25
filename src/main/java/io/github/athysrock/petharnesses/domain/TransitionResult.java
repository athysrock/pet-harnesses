package io.github.athysrock.petharnesses.domain;

public record TransitionResult(HarnessState state, TransitionError error) {
    public boolean success() { return error == TransitionError.NONE; }
    public static TransitionResult success(HarnessState state) { return new TransitionResult(state, TransitionError.NONE); }
    public static TransitionResult failure(HarnessState original, TransitionError error) { return new TransitionResult(original, error); }
}
