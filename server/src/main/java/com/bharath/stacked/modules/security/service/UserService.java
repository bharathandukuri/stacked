package com.bharath.stacked.modules.security.service;

import com.bharath.stacked.common.api.PageResponse;
import com.bharath.stacked.modules.security.dto.CreateUserRequest;
import com.bharath.stacked.modules.security.dto.UpdateUserRequest;
import com.bharath.stacked.modules.security.dto.UserFilterRequest;
import com.bharath.stacked.modules.security.dto.UserResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface UserService {

    @NonNull
    PageResponse<UserResponse> listUsers(@Nullable UserFilterRequest filter);

    @NonNull
    UserResponse getUserById(@NonNull String id);

    @NonNull
    UserResponse createUser(@NonNull CreateUserRequest request);

    @NonNull
    UserResponse updateUser(@NonNull String id, @NonNull UpdateUserRequest request, @Nullable String currentUserId);

    void deleteUser(@NonNull String id, @Nullable String currentUserId);
}

