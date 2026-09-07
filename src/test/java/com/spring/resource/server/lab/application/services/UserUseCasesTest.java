package com.spring.resource.server.lab.application.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.spring.resource.server.lab.application.command.CreateUserCommand;
import com.spring.resource.server.lab.application.command.UpdateUserCommand;
import com.spring.resource.server.lab.application.ports.CreateUserUseCase;
import com.spring.resource.server.lab.application.ports.DeleteUserUseCase;
import com.spring.resource.server.lab.application.ports.FindAllUserUseCase;
import com.spring.resource.server.lab.application.ports.FindUserByIdUseCase;
import com.spring.resource.server.lab.application.ports.SearchUserByUsernameUseCase;
import com.spring.resource.server.lab.application.ports.UpdateUserUseCase;
import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;
import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.exception.UserNotFoundException;
import com.spring.resource.server.lab.domain.model.User;

/// Tests of the six user-management use cases.
///
/// **No `@SpringBootTest`, no Keycloak and no PostgreSQL running**: each use case is instantiated
/// with `new` and the outbound port is replaced by [InMemoryUserRepository]. They run in
/// milliseconds and depend on nothing being started.
///
/// All six share the same in-memory repository, which is exactly what happens in production:
/// `ApplicationBeansConfig` injects the same `UserRepository` bean into them. That is why a user
/// created through `CreateUserUseCase` is visible from `FindAllUserUseCase`.
///
/// Contrast this with `ResourceServerLabApplicationTests`, which needs both infrastructures alive
/// just to check that the context loads.
class UserUseCasesTest {

    private InMemoryUserRepository repository;
    private InMemoryIdentityProvider identityProvider;

    private FindAllUserUseCase findAll;
    private FindUserByIdUseCase findById;
    private SearchUserByUsernameUseCase searchByUsername;
    private CreateUserUseCase create;
    private UpdateUserUseCase update;
    private DeleteUserUseCase delete;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        identityProvider = new InMemoryIdentityProvider();
        findAll = new FindAllUserService(repository);
        findById = new FindUserByIdService(repository);
        searchByUsername = new SearchUserByUsernameService(repository);
        create = new CreateUserService(repository, identityProvider);
        update = new UpdateUserService(repository, identityProvider);
        delete = new DeleteUserService(repository, identityProvider);
    }

    @Nested
    @DisplayName("CreateUserUseCase")
    class Creation {

        @Test
        @DisplayName("crea un usuario y le asigna identidad")
        void createsUser() {
            User created = create.execute(creationCommand("bpallasco", "bpallasco@example.com", Set.of("admin")));

            assertEquals("bpallasco", created.getUsername());
            assertEquals("bpallasco@example.com", created.getEmail().value());
            assertEquals(Set.of("ADMIN"), created.getRoles(), "el dominio normaliza los roles a mayúsculas");
            assertTrue(created.getId().startsWith("USE-"), "el id lo genera la estrategia del dominio");
            assertNotNull(created.getSsoId(), "el id de Keycloak viaja en ssoId");
            assertNotEquals(created.getId(), created.getSsoId(), "son dos identificadores distintos");
            assertEquals(1, identityProvider.registerCalls);
            assertEquals(1, repository.saveCalls);
        }

        @Test
        @DisplayName("aplica el rol por defecto cuando el alta no especifica ninguno")
        void appliesDefaultRole() {
            User created = create.execute(creationCommand("sinroles", "sinroles@example.com", Set.of()));

            assertEquals(Set.of(User.DEFAULT_ROLE), created.getRoles());
        }

        // El alta duplicada ya no se comprueba en el caso de uso: la cubre @Unique sobre
        // CreateUserRequest, que es una anotación del adaptador REST y no se puede ejercitar desde
        // aquí sin levantar contexto web. UpdateUserUseCase sí conserva la comprobación, y su test
        // sigue más abajo.

        @Test
        @DisplayName("rechaza un correo con formato inválido")
        void rejectsInvalidEmail() {
            assertThrows(InvalidUserDataException.class,
                    () -> create.execute(creationCommand("malcorreo", "esto-no-es-un-correo", Set.of())));

            assertEquals(0, repository.saveCalls);
            assertEquals(0, identityProvider.registerCalls);
        }

        @Test
        @DisplayName("registra la cuenta antes de guardar la fila")
        void registersTheAccountBeforeStoringTheRow() {
            User created = create.execute(creationCommand("conclave", "conclave@example.com", Set.of()));

            assertEquals(1, identityProvider.registerCalls);
            assertTrue(identityProvider.hasAccount(created.getSsoId()));
            assertEquals(0, identityProvider.unregisterCalls);
        }

        @Test
        @DisplayName("no guarda ninguna fila si el proveedor de identidad rechaza el alta")
        void storesNothingWhenRegistrationFails() {
            identityProvider.failureToThrow = new IllegalStateException("Keycloak rechazó el alta");

            assertThrows(IllegalStateException.class,
                    () -> create.execute(creationCommand("rechazado", "rechazado@example.com", Set.of())));

            assertEquals(0, repository.saveCalls);
        }

        @Test
        @DisplayName("da de baja la cuenta si luego falla el guardado de la fila")
        void unregistersTheAccountWhenStoringTheRowFails() {
            // Los dos sistemas no comparten transacción. Una cuenta registrada sin fila que la
            // represente ocuparía el nombre de usuario para siempre y esta aplicación no sabría
            // ni que existe, así que el alta se deshace.
            repository.failureToThrow = new IllegalStateException("La base de datos rechazó la fila");

            assertThrows(IllegalStateException.class,
                    () -> create.execute(creationCommand("huerfano", "huerfano@example.com", Set.of())));

            assertEquals(1, identityProvider.registerCalls);
            assertEquals(1, identityProvider.unregisterCalls);
            assertTrue(findAll.execute().isEmpty());
        }

        @Test
        @DisplayName("rechaza un alta sin contraseña")
        void rejectsCreationWithoutPassword() {
            CreateUserCommand withoutPassword = new CreateUserCommand(
                    "sinclave", "sinclave@example.com", "Ana", "Pérez", Set.of(), "  ");

            assertThrows(InvalidUserDataException.class, () -> create.execute(withoutPassword));
            assertEquals(0, repository.saveCalls);
        }
    }

    @Nested
    @DisplayName("UpdateUserUseCase")
    class Update {

        @Test
        @DisplayName("actualiza un usuario existente conservando su identidad")
        void updatesUser() {
            User created = create.execute(creationCommand("original", "original@example.com", Set.of()));

            update.execute(created.getId(), new UpdateUserCommand(
                    "original", "created@example.com", "Ana", "Pérez", Set.of("admin"), null));

            // El caso de uso ya no devuelve nada, así que el resultado se comprueba donde de
            // verdad importa: en lo que quedó guardado.
            User updated = findById.execute(created.getId());
            assertEquals("created@example.com", updated.getEmail().value());
            assertEquals(Set.of("ADMIN"), updated.getRoles());
            assertEquals(created.getId(), updated.getId());
            // Dos guardados: el del alta y el de esta modificacion. Ya no hay create y update por
            // separado, ambos pasan por save.
            assertEquals(2, repository.saveCalls);
        }

        @Test
        @DisplayName("no toca la cuenta de Keycloak si solo cambia el perfil")
        void doesNotTouchTheAccountWhenOnlyTheProfileChanges() {
            User created = create.execute(creationCommand("estable", "estable@example.com", Set.of("admin")));
            int accountCallsAfterCreation = identityProvider.updateAccountCalls;

            // Cambian nombre y apellido y nada mas: son datos de esta aplicacion, no de Keycloak.
            update.execute(created.getId(), new UpdateUserCommand(
                    "estable", "estable@example.com", "Otro", "Apellido", Set.of("admin"), null));

            assertEquals(accountCallsAfterCreation, identityProvider.updateAccountCalls);
            assertEquals(0, identityProvider.changePasswordCalls);
        }

        @Test
        @DisplayName("sincroniza la cuenta cuando cambia algo que Keycloak conoce")
        void syncsTheAccountWhenTheUsernameChanges() {
            User created = create.execute(creationCommand("viejo", "viejo@example.com", Set.of()));

            update.execute(created.getId(), new UpdateUserCommand(
                    "nuevo", "viejo@example.com", "Ana", "Perez", Set.of(), null));

            assertEquals(1, identityProvider.updateAccountCalls);
        }

        @Test
        @DisplayName("cambia la contrasena solo si el comando la trae")
        void changesThePasswordOnlyWhenTheCommandCarriesOne() {
            User created = create.execute(creationCommand("conclave", "conclave@example.com", Set.of()));

            update.execute(created.getId(), new UpdateUserCommand(
                    "conclave", "conclave@example.com", "Ana", "Perez", Set.of(), "   "));
            assertEquals(0, identityProvider.changePasswordCalls);

            update.execute(created.getId(), new UpdateUserCommand(
                    "conclave", "conclave@example.com", "Ana", "Perez", Set.of(), "otraClave"));
            assertEquals(1, identityProvider.changePasswordCalls);
            assertEquals("otraClave", identityProvider.passwordOf(created.getSsoId()));
        }

        @Test
        @DisplayName("falla sobre un usuario inexistente sin tocar el repositorio")
        void updatingUnknownUserFails() {
            UpdateUserCommand command = new UpdateUserCommand(
                    "fantasma", "fantasma@example.com", "Ana", "Pérez", Set.of(), null);

            assertThrows(UserNotFoundException.class, () -> update.execute("no-existe", command));
            assertEquals(0, repository.saveCalls);
        }

        @Test
        @DisplayName("impide renombrar a un nombre ya ocupado")
        void renamingToTakenUsernameFails() {
            create.execute(creationCommand("ocupado", "ocupado@example.com", Set.of()));
            User other = create.execute(creationCommand("libre", "libre@example.com", Set.of()));

            UpdateUserCommand command = new UpdateUserCommand(
                    "ocupado", "libre@example.com", "Ana", "Pérez", Set.of(), null);

            int savesBefore = repository.saveCalls;
            assertThrows(UserAlreadyExistsException.class, () -> update.execute(other.getId(), command));
            assertEquals(savesBefore, repository.saveCalls);
        }
    }

    @Nested
    @DisplayName("DeleteUserUseCase")
    class Deletion {

        @Test
        @DisplayName("elimina un usuario existente")
        void deletesUser() {
            User created = create.execute(creationCommand("aborrar", "aborrar@example.com", Set.of()));

            String ssoId = created.getSsoId();

            delete.execute(created.getId());

            // Borrado logico: la fila sigue ahi, pero findAll ya no la devuelve.
            assertEquals(1, repository.softDeleteCalls);
            assertTrue(findAll.execute().isEmpty());

            // Y la cuenta desaparece del proveedor, que es lo unico que permitiria volver a entrar.
            assertEquals(1, identityProvider.unregisterCalls);
            assertFalse(identityProvider.hasAccount(ssoId));

            // findById tampoco lo devuelve: un usuario dado de baja deja de existir para la API.
            assertThrows(UserNotFoundException.class, () -> findById.execute(created.getId()));
        }

        @Test
        @DisplayName("una segunda baja falla: el usuario ya no existe para la API")
        void deletingTwiceFails() {
            User created = create.execute(creationCommand("dosveces", "dosveces@example.com", Set.of()));
            delete.execute(created.getId());

            assertThrows(UserNotFoundException.class, () -> delete.execute(created.getId()));
            assertEquals(1, repository.softDeleteCalls);
            assertEquals(1, identityProvider.unregisterCalls);
        }

        @Test
        @DisplayName("falla sobre un usuario inexistente sin tocar el repositorio")
        void deletingUnknownUserFails() {
            assertThrows(UserNotFoundException.class, () -> delete.execute("no-existe"));
            assertEquals(0, repository.softDeleteCalls);
            assertEquals(0, identityProvider.unregisterCalls);
        }
    }

    @Nested
    @DisplayName("Casos de uso de consulta")
    class Queries {

        @Test
        @DisplayName("FindAllUserUseCase devuelve las altas hechas por CreateUserUseCase")
        void listsWhatWasCreated() {
            create.execute(creationCommand("uno", "uno@example.com", Set.of()));
            create.execute(creationCommand("dos", "dos@example.com", Set.of()));

            assertEquals(2, findAll.execute().size());
        }

        @Test
        @DisplayName("FindUserByIdUseCase falla sobre una identidad inexistente")
        void findingUnknownUserFails() {
            assertThrows(UserNotFoundException.class, () -> findById.execute("no-existe"));
        }

        @Test
        @DisplayName("SearchUserByUsernameUseCase encuentra por nombre exacto")
        void findsByUsername() {
            User created = create.execute(creationCommand("encontrable", "encontrable@example.com", Set.of()));

            User found = searchByUsername.execute("encontrable");

            assertEquals(created.getId(), found.getId());
            assertEquals("encontrable", found.getUsername());
        }

        @Test
        @DisplayName("SearchUserByUsernameUseCase falla si nadie tiene ese nombre")
        void searchingUnknownUsernameFails() {
            create.execute(creationCommand("encontrable", "encontrable@example.com", Set.of()));

            assertThrows(UserNotFoundException.class, () -> searchByUsername.execute("otro"));
        }

        @Test
        @DisplayName("SearchUserByUsernameUseCase no encuentra por subcadena: la busqueda es exacta")
        void searchDoesNotMatchASubstring() {
            create.execute(creationCommand("anabel", "anabel@example.com", Set.of()));

            assertThrows(UserNotFoundException.class, () -> searchByUsername.execute("nabe"));
        }

        @Test
        @DisplayName("SearchUserByUsernameUseCase rechaza un término vacío")
        void rejectsBlankSearchTerm() {
            assertThrows(InvalidUserDataException.class, () -> searchByUsername.execute("   "));
        }
    }

    private CreateUserCommand creationCommand(String username, String email, Set<String> roles) {
        return new CreateUserCommand(username, email, "Ana", "Pérez", roles, "contrasena-segura");
    }

}
