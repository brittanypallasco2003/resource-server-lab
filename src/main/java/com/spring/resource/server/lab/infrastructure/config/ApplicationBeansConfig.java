package com.spring.resource.server.lab.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spring.resource.server.lab.application.ports.CreateUserUseCase;
import com.spring.resource.server.lab.application.ports.DeleteUserUseCase;
import com.spring.resource.server.lab.application.ports.FindAllUserUseCase;
import com.spring.resource.server.lab.application.ports.FindUserByIdUseCase;
import com.spring.resource.server.lab.application.ports.SearchUserByUsernameUseCase;
import com.spring.resource.server.lab.application.ports.UpdateUserUseCase;
import com.spring.resource.server.lab.application.services.CreateUserService;
import com.spring.resource.server.lab.application.services.DeleteUserService;
import com.spring.resource.server.lab.application.services.FindAllUserService;
import com.spring.resource.server.lab.application.services.FindUserByIdService;
import com.spring.resource.server.lab.application.services.SearchUserByUsernameService;
import com.spring.resource.server.lab.application.services.UpdateUserService;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// Wiring of the application layer.
///
/// This is the counterpart of the use-case implementations carrying no `@Service`: if the
/// application may not depend on Spring, somebody has to register them, and that somebody is the
/// infrastructure.
///
/// It may look like bureaucracy, but it is exactly the point of the exercise. The framework stays
/// out of the core, and the proof is that every use case is instantiated with `new` in its unit
/// test without starting a Spring context.
///
/// The [UserRepository] they all receive is a single bean, so the use cases always operate over
/// the same store. The ones that also touch the account receive [IdentityProvider] as well.
@Configuration
public class ApplicationBeansConfig {

    /// Registers the use case that lists users.
    /// @param userRepository the active outbound adapter
    /// @return FindAllUserUseCase the use case
    @Bean
    FindAllUserUseCase findAllUserUseCase(UserRepository userRepository) {
        return new FindAllUserService(userRepository);
    }

    /// Registers the use case that looks a user up by identity.
    /// @param userRepository the active outbound adapter
    /// @return FindUserByIdUseCase the use case
    @Bean
    FindUserByIdUseCase findUserByIdUseCase(UserRepository userRepository) {
        return new FindUserByIdService(userRepository);
    }

    /// Registers the use case that searches users by username.
    /// @param userRepository the active outbound adapter
    /// @return SearchUserByUsernameUseCase the use case
    @Bean
    SearchUserByUsernameUseCase searchUserByUsernameUseCase(UserRepository userRepository) {
        return new SearchUserByUsernameService(userRepository);
    }

    /// Registers the use case that creates users.
    ///
    /// It receives both ports because creating a user touches two systems: the account is created
    /// in the identity provider and the row is stored in the repository. See [CreateUserService].
    ///
    /// @param userRepository the active outbound adapter
    /// @param identityProvider the identity provider
    /// @return CreateUserUseCase the use case
    @Bean
    CreateUserUseCase createUserUseCase(UserRepository userRepository, IdentityProvider identityProvider) {
        return new CreateUserService(userRepository, identityProvider);
    }

    /// Registers the use case that updates users.
    /// @param userRepository the active outbound adapter
    /// @param identityProvider the identity provider
    /// @return UpdateUserUseCase the use case
    @Bean
    UpdateUserUseCase updateUserUseCase(UserRepository userRepository, IdentityProvider identityProvider) {
        return new UpdateUserService(userRepository, identityProvider);
    }

    /// Registers the use case that deletes users.
    /// @param userRepository the active outbound adapter
    /// @param identityProvider the identity provider
    /// @return DeleteUserUseCase the use case
    @Bean
    DeleteUserUseCase deleteUserUseCase(UserRepository userRepository, IdentityProvider identityProvider) {
        return new DeleteUserService(userRepository, identityProvider);
    }

}
