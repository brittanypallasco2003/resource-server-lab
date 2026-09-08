package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.io.Serializable;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
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
/// storing and arrives ready-made through the mapper; the database only writes it down.
///
/// That has one consequence worth stating, because it looks like an omission: **this class
/// deliberately does not implement `Persistable`.** Spring Data picks `persist` or `merge` by
/// asking `isNew()`, whose default answer is "the id is null". Here the id is *never* null, so the
/// default always answers "not new" and every save goes through `merge` — one `SELECT` to see what
/// is in the table, then the `INSERT` or the `UPDATE`. Correct in both directions, at the price of
/// one query on inserts.
///
/// `Persistable` was tried, backed by a `@Transient boolean pendingInsert` cleared on `@PostLoad`
/// and `@PrePersist`. It cannot work in this codebase: the entity read from the database is mapped
/// to a domain `User` and thrown away, and a **brand-new** entity is built by the mapper for every
/// save. That fresh object never went through either callback, so it always claimed to be new, and
/// every update was attempted as an `INSERT` — a duplicate-key error on the second write of any
/// user. The memory lived in an object that gets discarded.
///
/// Re-adding it only pays off together with a repository port that separates creating from
/// updating, so the caller — which does know which one it is — can say so without a query. Until
/// then, `merge` deciding for itself is both simpler and the same cost.
///
/// @param <ID> type of the identifier
/// Read-only from the outside: there is **no setter for the id**. It is assigned once, through
/// the constructor, by the mapper translating from the domain. A `setId` would be inherited by
/// every entity, and since [#equals(Object)] and [#hashCode()] are built on the identifier,
/// reassigning it would strand the object in the wrong bucket of any hash-based collection.
@Getter
@MappedSuperclass
public abstract class BaseJpaEntity<ID extends Serializable> {

    @Id
    private ID id;

    /// No-argument constructor required by JPA.
    protected BaseJpaEntity() {
    }

    /// @param id the identifier already decided by the domain
    protected BaseJpaEntity(ID id) {
        this.id = id;
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
