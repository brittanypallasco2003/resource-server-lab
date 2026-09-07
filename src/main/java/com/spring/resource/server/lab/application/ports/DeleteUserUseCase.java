package com.spring.resource.server.lab.application.ports;


/// Interface that defines the operation for deleting a user. It is part of the application layer and serves as a contract for user-related use cases.
public interface DeleteUserUseCase {

    /// Executes the use case to delete a user.
    /// @param id the identity of the user to delete
    /// @throws com.spring.resource.server.lab.domain.exception.UserNotFoundException if no user has that identity
    void execute(String id);

}
