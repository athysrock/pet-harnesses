package io.github.athysrock.petharnesses.domain;

public final class HarnessDeathPolicy {
    private HarnessDeathPolicy() {}

    public static DeathOutcome onDeath(HarnessState state, boolean keepInventory, RestorePort restorePort) {
        if (state.phase() != HarnessPhase.OCCUPIED)
            return new DeathOutcome(state, DeathDisposition.UNCHANGED, TransitionError.NONE);
        if (keepInventory)
            return new DeathOutcome(state, DeathDisposition.RETAIN_OCCUPIED, TransitionError.NONE);
        TransitionResult restored = HarnessTransactions.restore(state, restorePort);
        if (!restored.success())
            return new DeathOutcome(restored.state(), DeathDisposition.RETAIN_OCCUPIED_RESTORE_FAILED, restored.error());
        return new DeathOutcome(restored.state(), DeathDisposition.DROP_LINKED, TransitionError.NONE);
    }
}
