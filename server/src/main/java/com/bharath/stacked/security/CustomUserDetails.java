package com.bharath.stacked.security;

import com.bharath.stacked.model.User;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(@NonNull User user) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
    }

    @Override
    @NonNull
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    @Nullable
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    @NonNull
    public String getUsername() {
        return user.getEmail();
    }

    @NonNull
    public User getUser() {
        return user;
    }

    @Nullable
    public String getId() {
        return user.getId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
