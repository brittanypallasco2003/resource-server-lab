package com.spring.resource.server.lab.application.ports;

import com.spring.resource.server.lab.domain.model.User;

/// Interface that defines the operation for looking a user up by their exact username. It is part
/// of the application layer and serves as a contract for user-related use cases.
public interface SearchUserByUsernameUseCase {

    /// Executes the use case to look a user up by their exact username.
    ///
    /// It returns the user itself and not a collection, because a username belongs to exactly one
    /// user: the `@Unique` constraint on the creation request and the `uk_users_username` index on
    /// the table both guarantee it. Returning a `Set` suggested otherwise and forced every caller
    /// to handle a case that cannot happen.
    ///
    /// Not finding one is reported the same way [FindUserByIdUseCase] reports it — with an
    /// exception, not with an empty result — so both lookups behave alike from the outside.
    ///
    /// @param username the exact username to look for
    /// @return User the user with that username
    /// @throws com.spring.resource.server.lab.domain.exception.InvalidUserDataException if the username is blank
    /// @throws com.spring.resource.server.lab.domain.exception.UserNotFoundException if nobody has that username
    User execute(String username);

}
