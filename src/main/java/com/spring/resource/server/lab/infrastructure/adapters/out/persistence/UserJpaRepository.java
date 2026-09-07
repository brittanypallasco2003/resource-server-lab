package com.spring.resource.server.lab.infrastructure.adapters.out.persistence;

import java.util.Optional;
import java.util.Set;

import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/// Repository of Spring Data over [UserJpaEntity].
/// Note that this is **not** the port: the port is
/// [com.spring.resource.server.lab.domain.repository.UserRepository], which lives in the domain and knows nothing about Spring Data.
/// This interface is an implementation detail used only by [JpaUserRepositoryAdapter].
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    /// The live user with exactly this username, if there is one.
    ///
    /// Equality and not `LIKE`: the username identifies a single user, guaranteed by the
    /// `uk_users_username` index on the table. A `LIKE` would also have made the input a pattern,
    /// so a search for `%` would have returned every row.
    ///
    /// @param username the exact username
    /// @return the user, or empty when nobody live has that username
    @Query("""
            SELECT u FROM UserJpaEntity u WHERE u.username = :username
            AND u.status <> 'DELETED'
            """)
    Optional<UserJpaEntity> findByUsername(String username);

    /// Tells whether a live user already has this username.
    ///
    /// The same question as [#findByUsername(String)] but answered with a `COUNT`, for callers that
    /// only need the yes or no. A soft-deleted user does not count, so deleting a user frees their
    /// username.
    ///
    /// @param username the exact username to check
    /// @return boolean true when a user that is not deleted has that username
    @Query("""
            SELECT COUNT(u) > 0 FROM UserJpaEntity u WHERE u.username = :username
            AND u.status <> 'DELETED'
            """)
    boolean existsByUsername(String username);

    /// Tells whether a live user already has exactly this e-mail address.
    /// @param email the exact address to check
    /// @return boolean true when a user that is not deleted has that address
    @Query("""
            SELECT COUNT(u) > 0 FROM UserJpaEntity u WHERE u.email = :email
            AND u.status <> 'DELETED'
            """)
    boolean existsByEmail(String email);

    /// Every user that has not been soft-deleted.
    /// @return the live users; empty when there are none
    @Query("""
            SELECT u FROM UserJpaEntity u WHERE u.status <> 'DELETED'
            """)
    Set<UserJpaEntity> findAllAndStatusNotDeleted();

    /// One live user by identity.
    ///
    /// It exists because the inherited `findById` would also return soft-deleted rows, and the
    /// contract of [com.spring.resource.server.lab.domain.repository.CrudRepository#findById]
    /// says a deleted aggregate comes back empty.
    ///
    /// @param id the identity
    /// @return the user, or empty when it does not exist or has been deleted
    @Query("""
            SELECT u FROM UserJpaEntity u WHERE u.id = :id
            AND u.status <> 'DELETED'
            """)
    Optional<UserJpaEntity> findByIdAndStatusNotDeleted(String id);
}
