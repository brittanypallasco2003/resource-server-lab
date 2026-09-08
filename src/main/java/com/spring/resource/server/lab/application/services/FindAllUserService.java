package com.spring.resource.server.lab.application.services;

import java.util.Set;

import com.spring.resource.server.lab.application.ports.FindAllUserUseCase;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [FindAllUserUseCase].
///
/// Carries no `@Service` annotation on purpose: the application layer may not depend on Spring.
/// It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class FindAllUserService implements FindAllUserUseCase {

    private final UserRepository userRepository;

    public FindAllUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Set<User> execute() {
        return userRepository.findAll();
    }

}
