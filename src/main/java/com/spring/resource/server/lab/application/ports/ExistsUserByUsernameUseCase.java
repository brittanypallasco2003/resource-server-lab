package com.spring.resource.server.lab.application.ports;


/// Interface that defines the operation for checking if a user exists by their exact username. It is part of the application layer and serves as a contract for user-related use cases.
public interface ExistsUserByUsernameUseCase {
    /// Executes the use case to check if a user exists by their exact username.
    /// @param username the username to check for existence
    /// @return boolean true if a user with the given username exists, false otherwise
    boolean execute(String username);

}
