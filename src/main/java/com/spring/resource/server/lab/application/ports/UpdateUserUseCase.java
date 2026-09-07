package com.spring.resource.server.lab.application.ports;

import com.spring.resource.server.lab.application.command.UpdateUserCommand;

/// Interface that defines the operation for updating an existing user. It is part of the application layer and serves as a contract for user-related use cases.
public interface UpdateUserUseCase {

    /// Executes the use case to update an existing user.
    /// @param id the identity of the user to update
    /// @param command the new data for the user
    void execute(String id, UpdateUserCommand command);

}
