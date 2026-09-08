package com.spring.resource.server.lab.domain.model.valueobject;

import java.util.regex.Pattern;

import com.spring.resource.server.lab.domain.validation.DomainValidator;

/// A user's e-mail address.
///
/// It is the only real business invariant a user has in this domain, which is why it is modelled
/// as a value object and not as a `String`. The validation lives here — and not only in the
/// inbound DTO — so that any future adapter (a CLI, a messaging consumer) inherits the same
/// guarantee without repeating the rule.
///
/// The pattern is declared here and not in [DomainValidator]: what shape an e-mail must have is a
/// rule of this field; applying a pattern and raising the error is the generic part.
public record Email(String value) {

    /// A deliberately lax check: `local@domain.tld` with no spaces. The goal is to discard obvious
    /// garbage, not to reimplement RFC 5322.
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");

    /// Field name used in the [DomainValidator] messages.
    private static final String FIELD = "El correo electrónico";

    public Email {
        value = DomainValidator.requireNotBlank(value, FIELD).toLowerCase();
        DomainValidator.requireFormat(value, FORMAT, FIELD);
    }

    /// Convenience factory to build the e-mail from a raw value.
    /// @param value the raw e-mail
    /// @return Email the normalized and validated e-mail
    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
