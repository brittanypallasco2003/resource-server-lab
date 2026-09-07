package com.spring.resource.server.lab.domain.exception;

/// Thrown when creating a user whose username is already taken. It is translated to a 409.
public class UserAlreadyExistsException extends DomainException {

    public UserAlreadyExistsException(String username) {
        super("Ya existe un usuario con el nombre de usuario '%s'".formatted(username));
    }

}
