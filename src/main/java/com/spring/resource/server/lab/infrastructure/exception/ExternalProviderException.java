package com.spring.resource.server.lab.infrastructure.exception;

/// Technical failure of an outbound adapter while talking to an external system.
///
/// It deliberately does **not** extend
/// [com.spring.resource.server.lab.domain.exception.DomainException]: Keycloak returning a 500 or
/// the connection dropping is not a violated business rule, and mixing the two would leave the REST
/// handler unable to tell a 4xx from a 5xx.
///
/// It lives in `infrastructure/exception` and not inside the Keycloak adapter so that the REST
/// error handler (an *inbound* adapter) can map it without importing anything from a concrete
/// *outbound* adapter: the same type is used by both Keycloak and JPA.
public class ExternalProviderException extends RuntimeException {

    public ExternalProviderException(String message) {
        super(message);
    }

    public ExternalProviderException(String message, Throwable cause) {
        super(message, cause);
    }

}
