package com.spring.resource.server.lab.application.ports;

import com.spring.resource.server.lab.domain.model.User;

/// Interface that defines the operation for finding a single user by its identity. It is part of the application layer and serves as a contract for user-related use cases.
public interface FindUserByIdUseCase {

    /// Executes the use case to find a user by its identity.
    /// @param id the identity to look up
    /// @return User the user found
    /// @throws com.spring.resource.server.lab.domain.exception.UserNotFoundException if no user has that identity
    User execute(String id);

}
