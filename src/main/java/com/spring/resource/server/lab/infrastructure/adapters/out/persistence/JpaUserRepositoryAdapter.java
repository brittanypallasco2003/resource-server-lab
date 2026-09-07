package com.spring.resource.server.lab.infrastructure.adapters.out.persistence;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.spring.resource.server.lab.domain.model.StatusEnum;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import com.spring.resource.server.lab.domain.exception.UserAlreadyExistsException;
import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UniqueField;
import com.spring.resource.server.lab.domain.repository.UserRepository;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.mapper.UserJpaMapper;

import static com.spring.resource.server.lab.domain.model.StatusEnum.DELETED;


/// The implementation of the [UserRepository] port that uses JPA to persist the data.
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserJpaMapper mapper;

    public JpaUserRepositoryAdapter(UserJpaRepository jpaRepository,
                                    UserJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Class<User> managedType() {
        return User.class;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<User> findAll() {
        return jpaRepository.findAllAndStatusNotDeleted().stream()
                .map(mapper::toDomain).collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(String id) {
        return jpaRepository.findByIdAndStatusNotDeleted(id).map(mapper::toDomain);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<User> searchByUsername(String username) {
        return jpaRepository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return jpaRepository.existsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByField(UniqueField field, String value) {
        return switch (field) {
            case USERNAME -> jpaRepository.existsByUsername(value);
            case EMAIL -> jpaRepository.existsByEmail(value);
        };
    }

    @Override
    /// Soft delete: the row stays, its status becomes [StatusEnum#DELETED].
    ///
    /// It needs its own transaction because it is a read-modify-write, and `save` resolves to a
    /// `merge` — a read of the row followed by the write.
    @Transactional
    public void softDelete(User model) {
        User deleted = model.withStatus(DELETED);
        jpaRepository.save(mapper.toEntity(deleted));
    }

    /// Stores the user, translating a duplicate username into a domain conflict.
    ///
    /// It uses `saveAndFlush` and not `save` on purpose. Hibernate defers the statement until the
    /// transaction flushes, which — with `@Transactional` — happens *after* this method returns,
    /// inside the proxy. The violation would then be raised where no `catch` of ours can see it,
    /// and the client would get a 500. Flushing here brings the failure inside the `try`.
    ///
    /// The cost is that the statement is sent at this point instead of being batched with whatever
    /// else the transaction is doing. With one row per call, there is nothing to batch.
    @Override
    @Transactional
    public User save(User user) {
        try {
            UserJpaEntity entity = jpaRepository.saveAndFlush(mapper.toEntity(user));
            return mapper.toDomain(entity);
        } catch (DataIntegrityViolationException e) {
            throw translate(e, user);
        }
    }

    /// Turns a database integrity error into the right kind of failure.
    ///
    /// Only the username index becomes a [UserAlreadyExistsException]: that collision is a business
    /// conflict and deserves a 409, the same call the Keycloak adapter makes when it sees a 409 of
    /// its own. Anything else — a null in a non-null column, a broken foreign key — is a defect in
    /// this application, not something the caller did, so it travels on untouched and surfaces as a
    /// 500 with its stack trace intact.
    ///
    /// The constraint is recognised **by name**, not by matching the message text. That is what
    /// keeps this from silently reclassifying an unrelated error the day a second index is added,
    /// and it is the same reason the domain has typed exceptions at all.
    ///
    /// @param e the error the driver reported
    /// @param user the user being stored, for the message
    /// @return the exception to throw: a domain conflict, or the original error
    private RuntimeException translate(DataIntegrityViolationException e, User user) {
        if (e.getCause() instanceof ConstraintViolationException violation
                && UserJpaEntity.USERNAME_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
            return new UserAlreadyExistsException(user.getUsername());
        }
        return e;
    }

    @Override
    /// Hard delete, leaving no audit trail. [#softDelete(User)] is what an ordinary business
    /// deletion should use.
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

}
