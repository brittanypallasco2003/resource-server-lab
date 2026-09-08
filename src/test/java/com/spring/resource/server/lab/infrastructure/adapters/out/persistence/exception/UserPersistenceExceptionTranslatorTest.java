package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.Set;

import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.model.valueobject.Email;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;

/// Tests of [UserPersistenceExceptionTranslator] with no Spring context and no database.
///
/// This is what the extraction bought. While these branches lived inside
/// `JpaUserRepositoryAdapter`, the only way to reach them was to provoke a real violation through
/// `@DataJpaTest` — which reaches exactly one of them, the one H2 happens to raise. Fabricating the
/// exception here reaches all of them, including the dialect decoration PostgreSQL produces and the
/// ones that must be left alone.
class UserPersistenceExceptionTranslatorTest {

    private final UserPersistenceExceptionTranslator translator = new UserPersistenceExceptionTranslator();

    private final User user = User.created("ana", Email.of("ana@example.com"), "Ana", "Perez", Set.of());

    @Test
    @DisplayName("el indice de username se convierte en un conflicto de negocio")
    void usernameIndexBecomesADomainConflict() {
        RuntimeException translated = translator.translate(
                integrityViolation(ConstraintKind.UNIQUE, UserJpaEntity.USERNAME_CONSTRAINT), user);

        UserAlreadyExistsException conflict = assertInstanceOf(UserAlreadyExistsException.class, translated);
        assertTrue(conflict.getMessage().contains("ana"), "el mensaje debe nombrar el username: " + conflict.getMessage());
    }

    @Test
    @DisplayName("reconoce el nombre decorado por el dialecto de H2")
    void recognisesTheNameDecoratedByH2() {
        // H2 no devuelve el nombre a secas, lo envuelve con el esquema y un sufijo generado. Esa
        // decoracion es la razon de que la comparacion sea 'contains' y no 'equals'.
        RuntimeException translated = translator.translate(
                integrityViolation(ConstraintKind.UNIQUE,
                        "PUBLIC.UK_USERS_USERNAME INDEX PUBLIC.UK_USERS_USERNAME_INDEX_4"),
                user);

        assertInstanceOf(UserAlreadyExistsException.class, translated);
    }

    @Test
    @DisplayName("reconoce el nombre en minusculas que reporta PostgreSQL")
    void recognisesTheLowercaseNamePostgresReports() {
        RuntimeException translated = translator.translate(
                integrityViolation(ConstraintKind.UNIQUE, "uk_users_username"), user);

        assertInstanceOf(UserAlreadyExistsException.class, translated);
    }

    @Test
    @DisplayName("otra restriccion viaja intacta y sigue siendo un 500")
    void anotherConstraintTravelsOnUntouched() {
        // Una clave foranea rota es un defecto de esta aplicacion, no algo que el cliente hizo mal.
        DataIntegrityViolationException original =
                integrityViolation(ConstraintKind.FOREIGN_KEY, "fk_user_roles_user");

        assertSame(original, translator.translate(original, user));
    }

    @Test
    @DisplayName("una columna no nula violada viaja intacta")
    void aNullInANonNullColumnTravelsOnUntouched() {
        DataIntegrityViolationException original = integrityViolation(ConstraintKind.NOT_NULL, null);

        assertSame(original, translator.translate(original, user));
    }

    @Test
    @DisplayName("un fallo de integridad sin causa de restriccion viaja intacto")
    void anIntegrityErrorWithoutAConstraintCauseTravelsOnUntouched() {
        DataIntegrityViolationException original =
                new DataIntegrityViolationException("fallo de integridad", new IllegalStateException("otra causa"));

        assertSame(original, translator.translate(original, user));
    }

    @Test
    @DisplayName("un nombre de restriccion que solo contiene el del indice como subcadena no confunde al traductor")
    void anUnrelatedNameIsNotMistakenForTheIndex() {
        DataIntegrityViolationException original =
                integrityViolation(ConstraintKind.UNIQUE, "uk_users_email");

        assertSame(original, translator.translate(original, user));
        assertEquals("uk_users_email",
                ((ConstraintViolationException) original.getCause()).getConstraintName());
    }

    /// Builds the error the way Spring hands it to the adapter: its own
    /// [DataIntegrityViolationException] wrapping Hibernate's, which is the only place the
    /// constraint name survives.
    ///
    /// @param kind the kind of constraint the database reported
    /// @param constraintName the name it reported, decorated as the dialect pleases
    /// @return DataIntegrityViolationException the error to translate
    private DataIntegrityViolationException integrityViolation(ConstraintKind kind, String constraintName) {
        ConstraintViolationException violation = new ConstraintViolationException(
                "violacion de restriccion", new SQLException("error del driver"), kind, constraintName);
        return new DataIntegrityViolationException(violation.getMessage(), violation);
    }

}
