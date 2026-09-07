package com.spring.resource.server.lab.domain.validation;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;

/// Validation and normalization primitives for the domain.
///
/// It gathers in one place **how** a field is checked and what message is produced when the check
/// fails. **What** each specific field requires is still declared in the value object that owns
/// it, which is where it can be guaranteed: as long as the call lives inside the compact
/// constructor, building an invalid `Email` is impossible. Moving these calls out of the
/// constructors and into the use cases would destroy that guarantee and let
/// `new Email("garbage")` compile and run.
///
/// The primitives return the sanitized value instead of only checking it, because half of these
/// rules normalize (trim, lowercase, default role). A `void` validator would leave that half
/// scattered across the callers.
///
/// The messages are written with gender-invariant Spanish ("no puede estar en blanco", "no puede
/// faltar") rather than constructions that have to agree ("vacío", "obligatorio"). They are
/// templates that interpolate the field name, so any gendered adjective would produce a malformed
/// sentence as soon as the field were feminine — "La contraseña no puede estar vacío".
public final class DomainValidator {

    private DomainValidator() {
    }

    /// Requires a value not to be null and returns it.
    ///
    /// @param <T> the type of the value being checked
    /// @param value the value to check
    /// @param field the field name, with its article, for the error message
    /// @return T the same value, already checked
    /// @throws InvalidUserDataException if the value is null
    public static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new InvalidUserDataException("%s no puede faltar".formatted(field));
        }
        return value;
    }

    /// Requires a string to have content and returns it trimmed.
    ///
    /// @param value the string to check
    /// @param field the field name, with its article, for the error message
    /// @return String the string without surrounding whitespace
    /// @throws InvalidUserDataException if the string is null or contains only whitespace
    public static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidUserDataException("%s no puede estar en blanco".formatted(field));
        }
        return value.trim();
    }

    /// Requires a string to match a given pattern and returns it.
    ///
    /// The pattern is the regular expression that defines the expected shape of the string. When
    /// the string does not match, the error message reports the invalid format.
    ///
    /// @param value the string to check
    /// @param pattern the pattern the string must match
    /// @param field the field name, with its article, for the error message
    /// @return String the same value, already checked
    /// @throws InvalidUserDataException if the value is null or does not match the pattern
    public static String requireFormat(String value, Pattern pattern, String field) {
        if (value == null || !pattern.matcher(value).matches()) {
            throw new InvalidUserDataException("%s tiene un formato inválido: '%s'".formatted(field, value));
        }
        return value;
    }

    /// Requires a string not to exceed a maximum length and returns it.
    ///
    /// Null is accepted: whether the field is mandatory is [#requireNotBlank(String, String)]'s
    /// question, not this one's.
    ///
    /// @param value the string to check
    /// @param max the maximum length allowed
    /// @param field the field name, with its article, for the error message
    /// @return String the same value, already checked
    /// @throws InvalidUserDataException if the value is not null and exceeds the maximum length
    public static String requireMaxLength(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new InvalidUserDataException(
                    "%s supera la longitud máxima de %d caracteres".formatted(field, max));
        }
        return value;
    }

    /// Normalizes a set of strings: trims, uppercases, drops blanks and duplicates, and applies a
    /// default value when the result is empty.
    ///
    /// Uppercase, and not lowercase, because the only caller is `User.roles` and an authority in
    /// Spring Security is conventionally written `ROLE_ADMIN`. Normalizing here is what lets
    /// `hasRole("ADMIN")` match whatever casing the client sent — `admin`, `Admin` or `ADMIN` all
    /// end up as the same role.
    ///
    /// The default value goes through the same normalization. Returning it raw would be the one
    /// way to get a role out of here that is not uppercase, which is exactly the kind of exception
    /// nobody remembers.
    ///
    /// @param values the set to normalize; null is treated as an empty set
    /// @param defaultValue the value to fall back to when the normalized set is empty; may be null
    /// @return Set<String> the normalized, immutable set, or the set holding only the default value
    public static Set<String> normalizeSet(Set<String> values, String defaultValue) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    normalized.add(value.trim().toUpperCase());
                }
            }
        }
        if (normalized.isEmpty()) {
            if (defaultValue == null || defaultValue.isBlank()) {
                return Set.of();
            }
            return Set.of(defaultValue.trim().toUpperCase());
        }
        return Set.copyOf(normalized);
    }

}
