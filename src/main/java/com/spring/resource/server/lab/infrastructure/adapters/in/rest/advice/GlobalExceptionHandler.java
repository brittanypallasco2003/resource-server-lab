package com.spring.resource.server.lab.infrastructure.adapters.in.rest.advice;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;
import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.exception.UserNotFoundException;
import com.spring.resource.server.lab.infrastructure.exception.ExternalProviderException;

/// Translates domain and infrastructure exceptions into HTTP responses.
///
/// Before this, the bare `RuntimeException` thrown by the Keycloak service came out as a 500 with
/// no body. Now every kind of failure has its own status code, and the client receives a
/// `ProblemDetail` (RFC 7807), the standard error format of Spring 6+ — there is no need to invent
/// an error record of our own.
///
/// This is also why the domain has typed exceptions at all: without them, choosing between 400,
/// 404 and 409 would mean inspecting message text.
///
/// It extends `ResponseEntityExceptionHandler` rather than declaring a loose `@ExceptionHandler`
/// for `Exception`. The reason is concrete: a catch-all without this base also swallows Spring
/// MVC's own exceptions — `NoResourceFoundException` among them — and would turn a 404 for an
/// unknown route into a 500. The base class already handles all of those and returns
/// `ProblemDetail`; only what is specific to this domain is added here.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String TIMESTAMP = "timestamp";

    /// Unknown user.
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 404
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Usuario no encontrado", ex.getMessage(), "usuario-no-encontrado");
    }

    /// Uniqueness conflict when creating or renaming a user.
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 409
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ProblemDetail> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, "El usuario ya existe", ex.getMessage(), "usuario-duplicado");
    }

    /// Violated domain invariant.
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 400
    @ExceptionHandler(InvalidUserDataException.class)
    public ResponseEntity<ProblemDetail> handleInvalidUserData(InvalidUserDataException ex) {
        return build(HttpStatus.BAD_REQUEST, "Datos de usuario inválidos", ex.getMessage(), "datos-invalidos");
    }

    /// A `@PreAuthorize` that evaluated to false, or any other authorization rule refused inside
    /// the servlet.
    ///
    /// Method security throws `AuthorizationDeniedException`, which extends `AccessDeniedException`,
    /// so catching the parent covers both. The exception is raised while the controller method is
    /// being invoked — that is, inside the `DispatcherServlet` — which is what lets this advice see
    /// it at all and answer with a `ProblemDetail`. Without this handler the catch-all below would
    /// degrade it to a 500, and a legitimate 403 would look like a server bug.
    ///
    /// The reply says only that permission is missing. It deliberately does not name the role that
    /// was required: telling an unauthorized caller what to obtain is help they have not earned.
    /// The full detail goes to the log.
    ///
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Acceso denegado",
                "No tiene permisos suficientes para realizar esta operación", "acceso-denegado");
    }

    /// Technical failure while talking to an external system (Keycloak, the database).
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 502
    @ExceptionHandler(ExternalProviderException.class)
    public ResponseEntity<ProblemDetail> handleExternalProvider(ExternalProviderException ex) {
        log.error("Fallo al comunicar con un proveedor externo", ex);
        return build(HttpStatus.BAD_GATEWAY, "Error del proveedor externo", ex.getMessage(), "proveedor-externo");
    }

    /// Safety net for any failure not covered above.
    ///
    /// It does not expose the original message: it could leak internal details. The real detail
    /// stays in the server log.
    ///
    /// @param ex the caught exception
    /// @return ResponseEntity<ProblemDetail> 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Error inesperado procesando la petición", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Se ha producido un error inesperado al procesar la petición", "error-interno");
    }

    /// Overrides Bean Validation handling to add the field-by-field breakdown, which is what an
    /// API consumer actually needs in order to fix the request.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.merge(error.getField(), error.getDefaultMessage(), (a, b) -> a + "; " + b);
        }

        ProblemDetail problem = problemDetail(HttpStatus.BAD_REQUEST, "Error de validación",
                "La petición contiene campos inválidos", "validacion");
        problem.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String title, String detail, String type) {
        return ResponseEntity.status(status).body(problemDetail(status, title, detail, type));
    }

    private ProblemDetail problemDetail(HttpStatus status, String title, String detail, String type) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:resource-server-lab:error:" + type));
        problem.setProperty(TIMESTAMP, Instant.now().toString());
        return problem;
    }

}
