package com.spring.resource.server.lab.domain.exception;

/// Thrown when a user's data violates a domain invariant.
///
/// It is raised by the value objects ([com.spring.resource.server.lab.domain.model.valueobject.Email])
/// and by the `User` aggregate itself. It is translated to a 400.
public class InvalidUserDataException extends DomainException {

    public InvalidUserDataException(String message) {
        super(message);
    }

}
