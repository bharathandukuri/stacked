package com.bharath.stacked.modules.execution.registry;

import com.bharath.stacked.modules.execution.dto.ManagedContainer;

import java.time.Instant;
import java.util.List;

public interface ManagedContainerRegistry {

    void register(ManagedContainer container);

    boolean isManaged(String containerId);

    ManagedContainer get(String containerId);

    void unregister(String containerId);

    List<ManagedContainer> findCreatedBefore(Instant cutoff);
}
