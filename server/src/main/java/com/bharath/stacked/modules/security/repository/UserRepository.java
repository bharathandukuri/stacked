package com.bharath.stacked.modules.security.repository;

import com.bharath.stacked.modules.security.model.Role;
import com.bharath.stacked.modules.security.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
