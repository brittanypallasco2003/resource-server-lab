package com.spring.resource.server.lab.infrastructure.adapters.out.persistence;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.spring.resource.server.lab.domain.model.StatusEnum;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa.UserJpaEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.DataIntegrityViolationException;

import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.UniqueField;
import com.spring.resource.server.lab.domain.repository.UserRepository;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.exception.UserPersistenceExceptionTranslator;
import com.spring.resource.server.lab.infrastructure.adapters.out.persistence.mapper.UserJpaMapper;

import static com.spring.resource.server.lab.domain.model.StatusEnum.DELETED;


/// The implementation of the [UserRepository] port that uses JPA to persist the data.
@Repository
public class JpaUserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserJpaMapper mapper;
    private final UserPersistenceExceptionTranslator exceptionTranslator;

    public JpaUserRepositoryAdapter(UserJpaRepository jpaRepository,
                                    UserJpaMapper mapper,
                                    UserPersistenceExceptionTranslator exceptionTranslator) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.exceptionTranslator = exceptionTranslator;
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
    ///
    /// *Which* failure the violation is, is not decided here: catching it is a persistence concern,
    /// naming it is a separate one, and [UserPersistenceExceptionTranslator] owns the second.
    ///
    /// @param user the user to store
    /// @return User the stored user, read back from the entity
    @Override
    @Transactional
    public User save(User user) {
        try {
            UserJpaEntity entity = jpaRepository.saveAndFlush(mapper.toEntity(user));
            return mapper.toDomain(entity);
        } catch (DataIntegrityViolationException e) {
            throw exceptionTranslator.translate(e, user);
        }
    }

    @Override
    /// Hard delete, leaving no audit trail. [#softDelete(User)] is what an ordinary business
    /// deletion should use.
    @Transactional
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }

}
