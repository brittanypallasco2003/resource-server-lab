package com.spring.resource.server.lab.application.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.spring.resource.server.lab.domain.model.StatusEnum;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UniqueField;
import com.spring.resource.server.lab.domain.repository.UserRepository;

/// In-memory implementation of the outbound port, for the application-layer tests.
///
/// That writing this takes thirty lines and requires neither Keycloak nor PostgreSQL nor Mockito
/// is the whole argument in favour of hexagonal architecture. The port is defined in the domain,
/// so a test can replace the outside world entirely.
class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> users = new LinkedHashMap<>();

    /// Counters used to check that the service does not call the repository when it should not.
    int saveCalls = 0;
    int deleteCalls = 0;
    int softDeleteCalls = 0;

    /// Failure [#save(User)] will throw on the next call, when not null. It makes it possible to
    /// exercise the compensation against the identity provider without needing a mock.
    RuntimeException failureToThrow;

    @Override
    public Class<User> managedType() {
        return User.class;
    }

    /// Users in [StatusEnum#DELETED] are left out: the contract of
    /// [com.spring.resource.server.lab.domain.repository.CrudRepository#findAll()] demands it.
    @Override
    public Set<User> findAll() {
        return users.values().stream()
                .filter(u -> u.getStatus() != StatusEnum.DELETED)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /// Deleted users are left out, just as in [#findAll()].
    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(users.get(id))
                .filter(u -> u.getStatus() != StatusEnum.DELETED);
    }

    /// Exact match, like the adapter's `WHERE u.username = :username`.
    ///
    /// Case-sensitive on purpose: `=` in PostgreSQL is, and a double that is kinder than the thing
    /// it stands in for is how a bug reaches production with the suite green.
    ///
    /// Deleted users are excluded here too, like in [#findAll()] and [#findById(String)].
    @Override
    public Optional<User> searchByUsername(String username) {
        return users.values().stream()
                .filter(u -> u.getStatus() != StatusEnum.DELETED)
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return searchByUsername(username).isPresent();
    }

    @Override
    public boolean existsByField(UniqueField field, String value) {
        return switch (field) {
            case USERNAME -> existsByUsername(value);
            case EMAIL -> users.values().stream()
                    .anyMatch(u -> u.getEmail().value().equalsIgnoreCase(value));
        };
    }

    /// Stores the row honouring the identifier the user carries.
    ///
    /// It used to overwrite it with a UUID of its own, imitating a store that generates the key.
    /// Not any more: the id is decided by the domain before anything leaves the application, and a
    /// repository that trampled on it would defeat the strategy of
    /// [com.spring.resource.server.lab.domain.identity.IdGenerator].
    @Override
    public User save(User user) {
        if (failureToThrow != null) {
            throw failureToThrow;
        }
        saveCalls++;
        users.put(user.getId(), user);
        return user;
    }

    /// Soft delete. It takes the whole aggregate and not its identity because that is how the port
    /// declares it: the caller already holds the user and the store does not need to read it back.
    @Override
    public void softDelete(User user) {
        softDeleteCalls++;
        users.put(user.getId(), user.withStatus(StatusEnum.DELETED));
    }

    @Override
    public void deleteById(String id) {
        deleteCalls++;
        users.remove(id);
    }

}
