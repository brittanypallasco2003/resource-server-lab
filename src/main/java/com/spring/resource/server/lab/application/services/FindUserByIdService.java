package com.spring.resource.server.lab.application.services;

import com.spring.resource.server.lab.application.ports.FindUserByIdUseCase;
import com.spring.resource.server.lab.domain.exception.UserNotFoundException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Implementation of [FindUserByIdUseCase].
///
/// Carries no `@Service` annotation on purpose: the application layer may not depend on Spring.
/// It is registered as a bean in `infrastructure/config/ApplicationBeansConfig`.
public class FindUserByIdService implements FindUserByIdUseCase {

    private final UserRepository userRepository;

    public FindUserByIdService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User execute(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

}
