package com.spring.resource.server.lab.application.services;

import java.util.Objects;

import com.spring.resource.server.lab.application.command.CreateUserCommand;
import com.spring.resource.server.lab.application.ports.CreateUserUseCase;
import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.model.valueobject.Email;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [CreateUserUseCase].
///
/// The sign-up touches two systems that cannot share a transaction: the identity provider, which
/// owns the account and the password, and the repository, which owns the row this application
/// reads. This service is the only place that knows both, and therefore the only place that can
/// put them back in order when one of them fails.
///
/// Order matters. The account is registered first, because that is the step that produces the
/// `ssoId`, and the row cannot be complete without it. The repository comes second, because
/// undoing a local row is cheap and reliable while undoing a remote account is neither.
///
/// It carries no `@Service` annotation on purpose: the application layer may not depend on
/// Spring. It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class CreateUserService implements CreateUserUseCase {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    public CreateUserService(UserRepository userRepository, IdentityProvider identityProvider) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    @Override
    public User  execute(CreateUserCommand command) {

        Objects.requireNonNull(command, "El comando de alta no puede ser nulo");

        if (command.password() == null || command.password().isBlank()) {
            throw new InvalidUserDataException("La contraseña no puede estar en blanco");
        }

        User user = User.created(
                command.username(),
                Email.of(command.email()),
                command.firstName(),
                command.lastName(),
                command.roles());

        String ssoId = identityProvider.register(user, command.password());



        try {
            return userRepository.save(user.withSsoId(ssoId));
        } catch (RuntimeException e) {
            identityProvider.unregister(ssoId);
            throw e;
        }
    }

}
