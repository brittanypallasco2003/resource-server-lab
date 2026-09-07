package com.spring.resource.server.lab.domain.repository;

import java.util.Set;

import com.spring.resource.server.lab.domain.model.User;

/// Output port: what the domain needs from the outside world to manage users.
///
/// No provider type appears here: that is what lets the implementation be replaced without
/// touching the core. Today it is `JpaUserRepositoryAdapter`; it was two adapters until Keycloak
/// stopped being a store and moved behind [IdentityProvider].
///
/// The generic CRUD lives in [CrudRepository]; only what is specific to users stays here. The
/// initial password is **not** part of this port: credentials travel through
/// [IdentityProvider], because only the identity provider can actually hold one.
public interface UserRepository extends CrudRepository<User, String> {

    /// Find users by their exact username.
    /// @param username the exact username to search for
    /// @return Set<User> the users with the given username; empty if none found
    Set<User> searchByUsername(String username);

    /// Checks if a user exists by their exact username.
    /// @param username the exact username to check for existence
    /// @return boolean true if a user with the given username exists, false otherwise
    boolean existsByUsername(String username);

}
