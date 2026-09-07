package com.spring.resource.server.lab.application.services;

import java.util.Set;

import com.spring.resource.server.lab.application.ports.SearchUserByUsernameUseCase;
import com.spring.resource.server.lab.domain.exception.InvalidUserDataException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [SearchUserByUsernameUseCase].
///
/// Carries no `@Service` annotation on purpose: the application layer may not depend on Spring.
/// It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class SearchUserByUsernameService implements SearchUserByUsernameUseCase {

    private final UserRepository userRepository;

    public SearchUserByUsernameService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<User> execute(String username) {
        if (username == null || username.isBlank()) {
            throw new InvalidUserDataException("El nombre de usuario a buscar no puede estar vacío");
        }
        return userRepository.searchByUsername(username.trim());
    }

}
