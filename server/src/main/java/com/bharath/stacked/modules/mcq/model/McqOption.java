package com.bharath.stacked.modules.mcq.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McqOption {

    @NonNull
    private String id;

    @NonNull
    private String text;

    @Nullable
    private String explanation;
}

