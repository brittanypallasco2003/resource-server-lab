package com.spring.resource.server.lab.domain.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;

/// Tests of the domain's validation primitives.
///
/// Now that every invariant goes through [DomainValidator], a bug here propagates to `User` and
/// `Email` at once. That is why it is tested directly and not only through the value objects that
/// use it.
class DomainValidatorTest {

    private static final String FIELD = "El correo electrónico";

    @Nested
    @DisplayName("requireNotBlank")
    class NotBlank {

        @Test
        @DisplayName("recorta los espacios sobrantes")
        void trimsSurroundingSpaces() {
            assertEquals("valor", DomainValidator.requireNotBlank("  valor  ", FIELD));
        }

        @Test
        @DisplayName("rechaza nulo, cadena vacía y solo espacios")
        void rejectsBlankValues() {
            assertThrows(InvalidUserDataException.class, () -> DomainValidator.requireNotBlank(null, FIELD));
            assertThrows(InvalidUserDataException.class, () -> DomainValidator.requireNotBlank("", FIELD));
            assertThrows(InvalidUserDataException.class, () -> DomainValidator.requireNotBlank("   ", FIELD));
        }

        @Test
        @DisplayName("el mensaje incluye el nombre del campo")
        void messageNamesTheField() {
            InvalidUserDataException e = assertThrows(InvalidUserDataException.class,
                    () -> DomainValidator.requireNotBlank(null, FIELD));

            assertTrue(e.getMessage().startsWith(FIELD), "mensaje inesperado: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("requireNotNull")
    class NotNull {

        @Test
        @DisplayName("devuelve el valor cuando no es nulo")
        void returnsTheValue() {
            Object value = new Object();
            assertEquals(value, DomainValidator.requireNotNull(value, FIELD));
        }

        @Test
        @DisplayName("acepta una cadena vacía: solo comprueba nulidad")
        void acceptsAnEmptyString() {
            assertEquals("", DomainValidator.requireNotNull("", FIELD));
        }

        @Test
        @DisplayName("rechaza nulo")
        void rejectsNull() {
            assertThrows(InvalidUserDataException.class, () -> DomainValidator.requireNotNull(null, FIELD));
        }
    }

    @Nested
    @DisplayName("requireFormat")
    class Format {

        private static final Pattern DIGITS_ONLY = Pattern.compile("^\\d+$");

        @Test
        @DisplayName("acepta un valor que encaja con el patrón")
        void acceptsAMatch() {
            assertEquals("12345", DomainValidator.requireFormat("12345", DIGITS_ONLY, FIELD));
        }

        @Test
        @DisplayName("rechaza un valor que no encaja y uno nulo")
        void rejectsANonMatch() {
            assertThrows(InvalidUserDataException.class,
                    () -> DomainValidator.requireFormat("12a45", DIGITS_ONLY, FIELD));
            assertThrows(InvalidUserDataException.class,
                    () -> DomainValidator.requireFormat(null, DIGITS_ONLY, FIELD));
        }
    }

    @Nested
    @DisplayName("requireMaxLength")
    class MaxLength {

        @Test
        @DisplayName("acepta longitudes iguales o menores al máximo")
        void acceptsUpToTheLimit() {
            assertEquals("abcde", DomainValidator.requireMaxLength("abcde", 5, FIELD));
        }

        @Test
        @DisplayName("rechaza longitudes por encima del máximo")
        void rejectsBeyondTheLimit() {
            assertThrows(InvalidUserDataException.class,
                    () -> DomainValidator.requireMaxLength("abcdef", 5, FIELD));
        }

        @Test
        @DisplayName("acepta nulo: la obligatoriedad la comprueba requireNotBlank")
        void acceptsNull() {
            assertEquals(null, DomainValidator.requireMaxLength(null, 5, FIELD));
        }
    }

    @Nested
    @DisplayName("normalizeSet")
    class NormalizeSet {

        @Test
        @DisplayName("recorta, pasa a mayúsculas y descarta blancos y nulos")
        void trimsUppercasesAndDropsBlanks() {
            Set<String> input = new LinkedHashSet<>();
            input.add("  admin ");
            input.add("User");
            input.add("   ");
            input.add(null);

            assertEquals(Set.of("ADMIN", "USER"), DomainValidator.normalizeSet(input, "defecto"));
        }

        @Test
        @DisplayName("aplica el valor por defecto cuando el conjunto queda vacío")
        void appliesTheDefaultValue() {
            assertEquals(Set.of("DEFECTO"), DomainValidator.normalizeSet(null, "defecto"));
            assertEquals(Set.of("DEFECTO"), DomainValidator.normalizeSet(Set.of(), "defecto"));
        }

        @Test
        @DisplayName("devuelve un conjunto vacío si no hay valor por defecto")
        void returnsEmptyWithoutADefault() {
            assertTrue(DomainValidator.normalizeSet(null, null).isEmpty());
            assertTrue(DomainValidator.normalizeSet(null, "   ").isEmpty());
        }

        @Test
        @DisplayName("el conjunto devuelto es inmutable")
        void returnsAnImmutableSet() {
            Set<String> result = DomainValidator.normalizeSet(Set.of("admin"), null);

            assertThrows(UnsupportedOperationException.class, () -> result.add("otro"));
        }
    }

}
