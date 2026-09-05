package com.spring.resource.server.lab.application.ports;

import com.spring.resource.server.lab.application.command.CreateUserCommand;
import com.spring.resource.server.lab.domain.model.User;

/// Interface that defines the operation for creating a new user. It is part of the application layer and serves as a contract for user-related use cases.
public interface CreateUserUseCase {

    /// Executes the use case to create a new user.
    /// @param command the data required to create the user
    /// @return User the created user, already carrying its identity
    /// @throws com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException if the username is already taken
    User execute(CreateUserCommand command);

}
