package com.spring.resource.server.lab.application.services;

import com.spring.resource.server.lab.application.ports.DeleteUserUseCase;
import com.spring.resource.server.lab.domain.exception.UserNotFoundException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [DeleteUserUseCase].
///
/// The deletion touches both systems: the account disappears from the identity provider and the
/// row is marked as deleted without leaving the database. That asymmetry is deliberate — the row
/// is kept because the audit trail is precisely what is interesting about a deleted user, while
/// the account has to go because it is the only thing that would still let that person log in.
///
/// Here Keycloak goes **first**, the other way round from the update, for two reasons. The first
/// is security: if the row were marked and the account stayed alive, that person could keep asking
/// for tokens, and this resource server would accept them without looking at the database again.
/// The second is that this order can be retried — if the soft delete fails, the account is already
/// gone but the user is still reachable, and repeating the operation finishes it, because
/// [IdentityProvider#unregister(String)] does not complain about an account that no longer exists.
///
/// It carries no `@Service` annotation on purpose: the application layer may not depend on Spring.
/// It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class DeleteUserService implements DeleteUserUseCase {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    public DeleteUserService(UserRepository userRepository, IdentityProvider identityProvider) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    @Override
    public void execute(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        if (user.getSsoId() != null) {
            identityProvider.unregister(user.getSsoId());
        }

        userRepository.softDelete(user);
    }

}
