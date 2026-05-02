package com.project.grcplatform.constant;

import java.util.Set;
import java.util.Map;

public enum LifecycleStatus {
    ACQUISITION,
    ACTIVE,
    MAINTENANCE,
    RETIRED,
    DECOMMISSIONED;

    private static final Map<LifecycleStatus, Set<LifecycleStatus>> ALLOWED = Map.of(
            ACQUISITION,    Set.of(ACTIVE),
            ACTIVE,         Set.of(MAINTENANCE, RETIRED),
            MAINTENANCE,    Set.of(ACTIVE, RETIRED),
            RETIRED,        Set.of(DECOMMISSIONED),
            DECOMMISSIONED, Set.of()
    );

    public boolean canTransitionTo(LifecycleStatus next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
}
