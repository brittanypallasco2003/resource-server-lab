package com.spring.resource.server.lab.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.model.StatusEnum;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.model.valueobject.Email;
import com.spring.resource.server.lab.domain.repository.UniqueField;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.exception.UserPersistenceExceptionTranslator;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.mapper.UserJpaMapperImpl;

/// Tests of [JpaUserRepositoryAdapter] against a real JPA provider on an in-memory database.
///
/// It exists because [com.spring.resource.server.lab.application.services.InMemoryUserRepository]
/// cannot catch a whole class of failure. That double stores users in a `Map`, where `put` behaves
/// identically for an insert and an update — so a persistence bug that only shows up when a row
/// already exists passes there and breaks in production. Two of the bugs this suite now pins down
/// did exactly that.
///
/// It needs no Keycloak and no PostgreSQL: `@DataJpaTest` swaps in an embedded database, and the
/// properties below keep the slice from resolving the Keycloak placeholders `application.properties`
/// declares, so the suite does not depend on a `.env` being present.
///
/// The adapter, the generated mapper and the exception translator are `@Import`ed because
/// `@DataJpaTest` loads entities and Spring Data repositories only — a `@Repository` or
/// `@Component` of our own is not part of the slice.
@DataJpaTest
@Import({ JpaUserRepositoryAdapter.class, UserJpaMapperImpl.class, UserPersistenceExceptionTranslator.class })
@TestPropertySource(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "jwt.auth.converter.principal-attribute=preferred_username",
        "jwt.auth.converter.resource-id=test-client",
        "keycloak.admin.realm-name=test-realm",
        "keycloak.admin.app-client-id=test-client",
        "keycloak.admin.user=test",
        "keycloak.admin.password=test",
        "keycloak.admin.client-secret=test"
})
class JpaUserRepositoryAdapterTest {

    @Autowired
    private JpaUserRepositoryAdapter adapter;

    @Autowired
    private UserJpaRepository jpaRepository;

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("guardar dos veces el mismo usuario actualiza la fila, no inserta otra")
        void savingTwiceUpdatesTheRow() {
            // Esta es la regresion que motivo el test. Con Persistable, la segunda llamada
            // intentaba un INSERT sobre un id que ya existia y reventaba con clave duplicada.
            User created = adapter.save(newUser("ana", "ana@example.com"));

            adapter.save(created.withData("ana", Email.of("otra@example.com"), "Ana", "Perez",
                    Set.of("ADMIN")));

            assertEquals(1, jpaRepository.count(), "no debe haberse insertado una segunda fila");
            User stored = adapter.findById(created.getId()).orElseThrow();
            assertEquals("otra@example.com", stored.getEmail().value());
            assertEquals(Set.of("ADMIN"), stored.getRoles());
        }

        @Test
        @DisplayName("conserva el identificador que decidio el dominio")
        void keepsTheDomainIdentifier() {
            User user = newUser("conid", "conid@example.com");

            User saved = adapter.save(user);

            assertEquals(user.getId(), saved.getId());
            assertTrue(saved.getId().startsWith("USE-"));
            assertTrue(jpaRepository.findById(user.getId()).isPresent());
        }

        @Test
        @DisplayName("conserva la fecha de creacion al actualizar")
        void keepsTheCreationTimestamp() {
            User created = adapter.save(newUser("audit", "audit@example.com"));

            adapter.save(created.withData("audit", Email.of("audit2@example.com"), "A", "B", Set.of()));

            UserJpaEntity row = jpaRepository.findById(created.getId()).orElseThrow();
            assertEquals(created.getAuditInfo().createdAt(), row.getAuditInfo().getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Lecturas y borrado logico")
    class Reads {

        @Test
        @DisplayName("findById no devuelve un usuario borrado logicamente")
        void findByIdHidesDeletedUsers() {
            User created = adapter.save(newUser("borrado", "borrado@example.com"));

            adapter.softDelete(created);

            assertTrue(adapter.findById(created.getId()).isEmpty(),
                    "el contrato de CrudRepository#findById excluye los borrados");
        }

        @Test
        @DisplayName("el borrado logico conserva la fila con estado DELETED")
        void softDeleteKeepsTheRow() {
            User created = adapter.save(newUser("rastro", "rastro@example.com"));

            adapter.softDelete(created);

            assertEquals(1, jpaRepository.count(), "la fila se conserva para la auditoria");
            assertEquals(StatusEnum.DELETED, jpaRepository.findById(created.getId()).orElseThrow().getStatus());
        }

        @Test
        @DisplayName("findAll excluye los borrados")
        void findAllHidesDeletedUsers() {
            User vivo = adapter.save(newUser("vivo", "vivo@example.com"));
            User muerto = adapter.save(newUser("muerto", "muerto@example.com"));

            adapter.softDelete(muerto);

            Set<User> all = adapter.findAll();
            assertEquals(1, all.size());
            assertEquals(vivo.getId(), all.iterator().next().getId());
        }

        @Test
        @DisplayName("searchByUsername compara el nombre exacto, no una subcadena")
        void searchMatchesTheExactUsername() {
            User created = adapter.save(newUser("anabel", "anabel@example.com"));

            assertEquals(created.getId(), adapter.searchByUsername("anabel").orElseThrow().getId());
            assertTrue(adapter.searchByUsername("nabe").isEmpty(), "'nabe' no es 'anabel'");
            assertTrue(adapter.searchByUsername("zzz").isEmpty());
        }

        @Test
        @DisplayName("searchByUsername no devuelve un usuario borrado logicamente")
        void searchHidesDeletedUsers() {
            User created = adapter.save(newUser("fantasma", "fantasma@example.com"));

            adapter.softDelete(created);

            assertTrue(adapter.searchByUsername("fantasma").isEmpty());
        }
    }

    @Nested
    @DisplayName("existsByField")
    class Uniqueness {

        @Test
        @DisplayName("detecta el nombre de usuario y el correo ocupados")
        void detectsTakenValues() {
            adapter.save(newUser("ocupado", "ocupado@example.com"));

            assertTrue(adapter.existsByField(UniqueField.USERNAME, "ocupado"));
            assertTrue(adapter.existsByField(UniqueField.EMAIL, "ocupado@example.com"));
            assertFalse(adapter.existsByField(UniqueField.USERNAME, "libre"));
        }

        @Test
        @DisplayName("un username duplicado es un conflicto de negocio, no un fallo tecnico")
        void duplicateUsernameBecomesADomainConflict() {
            // Ni @Unique ni la comprobacion de UpdateUserService protegen de una carrera: las dos
            // leen antes de escribir. El indice uk_users_username es la ultima defensa, y su
            // violacion tiene que salir como 409 y no como un 500 sin cuerpo.
            adapter.save(newUser("duplicada", "una@example.com"));

            User otra = newUser("duplicada", "otra@example.com");

            UserAlreadyExistsException e = assertThrows(UserAlreadyExistsException.class,
                    () -> adapter.save(otra));
            assertTrue(e.getMessage().contains("duplicada"), "mensaje inesperado: " + e.getMessage());
        }

        @Test
        @DisplayName("un usuario borrado libera su nombre de usuario")
        void deletedUsersDoNotOccupyTheirUsername() {
            User created = adapter.save(newUser("reciclable", "reciclable@example.com"));

            adapter.softDelete(created);

            assertFalse(adapter.existsByField(UniqueField.USERNAME, "reciclable"));
        }
    }

    private User newUser(String username, String email) {
        return User.created(username, Email.of(email), "Ana", "Perez", Set.of());
    }

}
