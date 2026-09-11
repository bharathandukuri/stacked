package com.bharath.stacked.repository;

import com.bharath.stacked.model.Role;
import com.bharath.stacked.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);
}
