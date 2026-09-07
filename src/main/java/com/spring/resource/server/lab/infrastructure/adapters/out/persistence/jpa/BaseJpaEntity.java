package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.io.Serializable;

import lombok.Setter;
import org.springframework.data.domain.Persistable;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;

/// Root of the JPA entities, the mirror of
/// [com.spring.resource.server.lab.domain.model.BaseModel] on the persistence side.
///
/// It is `@MappedSuperclass` and not `@Entity`: no table of its own corresponds to it, it only
/// contributes the identifier column to the tables of whoever extends it. Without that annotation
/// JPA would not even see the `id` field and would start up complaining that the entity has no
/// identifier.
///
/// The identifier is generic just as it is in the domain. Hibernate works out its type by looking
/// at the argument the concrete entity fixes (`UserJpaEntity extends BaseJpaEntity<String>`), so
/// declaring it once here is enough.
///
/// **There is no `@GeneratedValue` on purpose.** The identifier is decided in the domain before
/// storing and arrives ready-made through the mapper; the database only writes it down. That is
/// also the reason for implementing [Persistable]: see [#isNew()].
///
/// @param <ID> type of the identifier
@Getter
@Setter
@MappedSuperclass
public abstract class BaseJpaEntity<ID extends Serializable> implements Persistable<ID> {

    @Id
    private ID id;

    /// Flag meaning "not in the table yet". It is `@Transient`, so it does not exist as a column:
    /// it lives only while the object is in memory.
    @Transient
    private boolean pendingInsert = true;

    /// No-argument constructor required by JPA.
    protected BaseJpaEntity() {
    }

    /// @param id the identifier already decided by the domain
    protected BaseJpaEntity(ID id) {
        this.id = id;
    }

    /// Tells Spring Data whether the entity is an insert or an update.
    ///
    /// Without this, Spring Data decides by looking at whether the id is null. Since the id here is
    /// **always** set, it would always assume the row already exists: every insert would end up in
    /// a `merge`, which issues a `SELECT` to check before it can `INSERT`. With the flag, an insert
    /// is a plain `INSERT`.
    @Override
    public boolean isNew() {
        return pendingInsert;
    }

    /// Clears the flag as soon as the row really exists: when inserting it or when reading it
    /// back from the table.
    @PostLoad
    @PrePersist
    void markAsStored() {
        this.pendingInsert = false;
    }

    /// Two entities are the same when they share an identifier, just as in the domain.
    ///
    /// The comparison uses `instanceof` and not `getClass()` because Hibernate sometimes hands back
    /// a proxy — an invisible subclass generated on the fly — and with `getClass()` an entity would
    /// never equal its own proxy.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseJpaEntity<?> other) || id == null) {
            return false;
        }
        return id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

}
