package io.github.athysrock.petharnesses.transaction;

public enum CaptureResult {
    SUCCESS,
    NOT_LINKED,
    INVALID_LINK,
    PET_UNAVAILABLE,
    DUPLICATE_REPRESENTATION,
    SERIALIZATION_FAILED,
    COMMIT_FAILED,
    REMOVAL_RECONCILIATION_REQUIRED
}
