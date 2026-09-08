package com.spring.resource.server.lab.domain.repository;

import java.util.Optional;

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

    /// Finds the user with **exactly** this username.
    ///
    /// It returns at most one because a username identifies a single user: the `@Unique`
    /// constraint on the creation request and the `uk_users_username` index on the table both say
    /// so. Returning a collection would have suggested otherwise and forced every caller to handle
    /// a case that cannot happen.
    ///
    /// Deleted users are excluded, like in every other read of this port — so a soft-deleted
    /// username reads as free.
    ///
    /// @param username the exact username to look for
    /// @return Optional<User> the user, or empty when nobody has that username
    Optional<User> searchByUsername(String username);

    /// Tells whether a user already has this username.
    ///
    /// The same question as [#searchByUsername(String)], asked when the user itself is not needed:
    /// it answers with a `COUNT` instead of loading and mapping a row. A soft-deleted user does not
    /// count, so deleting a user frees their username.
    ///
    /// @param username the exact username to check
    /// @return boolean true when a live user has that username
    boolean existsByUsername(String username);

}
