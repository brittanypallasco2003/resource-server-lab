package com.spring.resource.server.lab.application.ports;

import java.util.Set;

import com.spring.resource.server.lab.domain.model.User;

/// Interface that defines the operation for finding all users in the system. It is part of the application layer and serves as a contract for user-related use cases.
public interface FindAllUserUseCase {

    /// Executes the use case to find all users in the system.
    /// @return Set<User> a list containing all users in the system
    Set<User> execute();

}
