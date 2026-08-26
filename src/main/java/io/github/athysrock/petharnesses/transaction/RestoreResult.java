package io.github.athysrock.petharnesses.transaction;

public enum RestoreResult {
    SUCCESS,
    NOT_OCCUPIED,
    INVALID_PAYLOAD,
    DUPLICATE_UUID,
    NO_SAFE_POSITION,
    RECONSTRUCTION_FAILED,
    INSERTION_FAILED,
    CLEAR_RECONCILIATION_REQUIRED
}
