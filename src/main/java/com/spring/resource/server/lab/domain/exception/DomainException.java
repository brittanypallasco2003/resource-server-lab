package com.spring.resource.server.lab.domain.exception;

/// Root of the domain's business exceptions.
///
/// It marks the failures that represent a violated business rule, as opposed to the technical
/// failures of an adapter (Keycloak down, network error, database timeout). That distinction is
/// what lets the REST error handler pick a 4xx instead of a 5xx without inspecting message text.
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }

}
