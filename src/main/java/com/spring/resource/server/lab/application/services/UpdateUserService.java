package com.spring.resource.server.lab.application.services;

import java.util.Objects;

import com.spring.resource.server.lab.application.command.UpdateUserCommand;
import com.spring.resource.server.lab.application.ports.UpdateUserUseCase;
import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.exception.UserNotFoundException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.model.valueobject.Email;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [UpdateUserUseCase].
///
/// The database goes first, the other way round from the creation. There Keycloak came first
/// because it was the step that produced the `ssoId`; here it produces nothing, and undoing works
/// differently: the creation undid itself by deleting, which is reliable, whereas here the previous
/// data would have to be restored, which can fail in turn. With the row already saved in its own
/// transaction, a provider failure leaves a clean error and the command can be retried as is,
/// because it is idempotent.
///
/// It resolves the existing user through [UserRepository] and not through
/// [com.spring.resource.server.lab.application.ports.FindUserByIdUseCase]: use cases depend on the
/// outbound port, never on each other, so each one stays independently testable.
///
/// It carries no `@Service` annotation on purpose: the application layer may not depend on Spring.
/// It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class UpdateUserService implements UpdateUserUseCase {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    public UpdateUserService(UserRepository userRepository, IdentityProvider identityProvider) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    @Override
    public void execute(String id, UpdateUserCommand command) {
        Objects.requireNonNull(command, "El comando de actualización no puede ser nulo");

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (!existing.getUsername().equalsIgnoreCase(command.username())
                && userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException(command.username());
        }

        User updated = existing.withData(
                command.username(),
                Email.of(command.email()),
                command.firstName(),
                command.lastName(),
                command.roles());

        userRepository.save(updated);

    
        if (affectsAccount(existing, updated)) {
            identityProvider.updateAccount(existing.getSsoId(), updated);
        }

    
        if (command.password() != null && !command.password().isBlank()) {
            identityProvider.changePassword(existing.getSsoId(), command.password());
        }
    }

    /// Tells whether the change affects anything the identity provider knows about.
    ///
    /// Those are the four pieces of data that travel to the account: username, e-mail, whether it
    /// is enabled, and the roles. Everything else — given name, family name — is this
    /// application's data, and checking it here is what avoids a network call on the most frequent
    /// update of all.
    ///
    /// @param before the user as it was
    /// @param after the user once updated
    /// @return boolean `true` when the account has to be synchronized
    private boolean affectsAccount(User before, User after) {
        return !before.getUsername().equals(after.getUsername())
                || !before.getEmail().equals(after.getEmail())
                || before.isEnabled() != after.isEnabled()
                || !before.getRoles().equals(after.getRoles());
    }

}
