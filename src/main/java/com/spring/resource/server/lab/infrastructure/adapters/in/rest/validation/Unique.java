package com.spring.resource.server.lab.infrastructure.adapters.in.rest.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.spring.resource.server.lab.domain.repository.UniqueField;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/// Rejects a value that is already taken in the store.
///
/// It is generic by design: it mentions neither `User` nor `UserRepository`. The aggregate is
/// named with `entity`, the field with [UniqueField], and [UniqueValidator] works out which of the
/// registered repositories to ask. Adding a second aggregate forces no change to the annotation
/// nor to the validator.
///
/// It lives in the inbound adapter and not in the domain because `jakarta.validation` is
/// technology, and `HexagonalArchitectureTest` forbids it from entering `domain/` or
/// `application/`.
///
/// **It only protects the HTTP path.** `@Valid` is a Spring MVC mechanism: any adapter calling the
/// use case directly skips it and will need a check of its own.
@Documented
@Constraint(validatedBy = UniqueValidator.class)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Unique {

    /// The aggregate uniqueness is checked against.
    /// @return Class<?> the aggregate's class
    Class<?> entity();

    /// The aggregate field that must be unique.
    /// @return UniqueField the field
    UniqueField field();

    /// @return String the message returned when the value is already taken
    String message() default "El valor ya está ocupado";

    /// @return Class<?>[] the validation groups
    Class<?>[] groups() default {};

    /// @return Class<? extends Payload>[] the constraint metadata
    Class<? extends Payload>[] payload() default {};

}
