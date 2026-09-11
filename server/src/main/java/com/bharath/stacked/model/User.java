package com.bharath.stacked.model;

import com.bharath.stacked.validation.ValidEmail;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @Nullable
    private String id;

    @NotBlank(message = "Name is required")
    private String name;

    @Indexed(unique = true)
    @ValidEmail(message = "Invalid email address")
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    private String password;

    @Builder.Default
    private Role role = Role.ADMIN;

    @CreatedDate
    @Nullable
    private Instant createdAt;

    @LastModifiedDate
    @Nullable
    private Instant updatedAt;
}
