package com.spring.resource.server.lab.application.ports;

import java.util.Set;

import com.spring.resource.server.lab.domain.model.User;

/// Interface that defines the operation for searching users by their exact username. It is part of the application layer and serves as a contract for user-related use cases.
public interface SearchUserByUsernameUseCase {

    /// Executes the use case to search users by exact username.
    /// @param username the username to search for
    /// @return Set<User> the matching users; empty if there are none
    /// @throws com.spring.resource.server.lab.domain.exception.InvalidUserDataException if the username is blank
    Set<User> execute(String username);

}
