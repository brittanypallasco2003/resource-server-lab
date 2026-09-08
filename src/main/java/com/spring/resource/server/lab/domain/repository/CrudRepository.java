package com.spring.resource.server.lab.domain.repository;

import java.util.Optional;
import java.util.Set;

/// Outbound contract shared by every aggregate: what any repository can do without knowing which
/// aggregate it holds.
///
/// `save` is generic because a credential travels through its own port, [IdentityProvider]. No
/// aggregate has to drag along a parameter that only one adapter would know what to do with.
///
/// @param <T> the aggregate type this repository manages
/// @param <ID> the type of its identity
public interface CrudRepository<T, ID> {

    /// The aggregate type this implementation manages.
    ///
    /// It is needed because Java generics are erased at runtime: a validator handed a
    /// `List<CrudRepository<?, ?>>` cannot ask an instance what its `T` was, so the implementation
    /// states it explicitly.
    ///
    /// @return Class<T> the aggregate's class
    Class<T> managedType();

    /// Returns every live aggregate.
    ///
    /// **Aggregates in [com.spring.resource.server.lab.domain.model.StatusEnum#DELETED] are not
    /// included.** A soft deletion keeps the row for the audit trail, not for listings; if they
    /// showed up here a deleted user would stay visible through the API and the deletion would
    /// have achieved nothing. This is part of the port's contract, so each adapter honours it with
    /// whatever its store can do — a condition in the query, a filter in memory.
    ///
    /// @return Set<T> the aggregates that are not deleted; empty when there are none
    Set<T> findAll();

    /// Looks a live aggregate up by identity.
    ///
    /// Same rule as [#findAll()]: an aggregate in
    /// [com.spring.resource.server.lab.domain.model.StatusEnum#DELETED] comes back empty. One rule
    /// for both reads means no caller has to remember which of the two hides them.
    ///
    /// The consequence is worth stating: deleting or updating an already deleted aggregate raises
    /// a not-found error rather than acting on a row nobody should be touching. That is the
    /// intended behaviour, not a side effect.
    ///
    /// Reading a deleted aggregate — for an audit screen, say — needs its own operation, declared
    /// as such, so that reaching for the trail is always a deliberate act.
    ///
    /// @param id the identity
    /// @return Optional<T> the aggregate, or empty when it does not exist or has been deleted
    Optional<T> findById(ID id);

    /// Stores an aggregate, new or already existing, and returns it as it ended up stored.
    ///
    /// It replaces the former `create(T)` and `update(ID, T)`. With a single method there is no
    /// longer any way for the identifier passed alongside and the one carried inside the aggregate
    /// to disagree; there is only one now, and it is the aggregate's own.
    ///
    /// Telling an insert from an update is the adapter's business, and it is the reason this port
    /// has one method instead of two. The JPA adapter lets `merge` resolve it: one query to see
    /// what the table holds, then the insert or the update. Should that query ever be worth
    /// removing, the honest fix is to split this method in two so the caller — which knows which
    /// one it is — can say so; guessing inside the adapter is what produced a duplicate-key error
    /// on every update once before.
    ///
    /// @param aggregate the aggregate to store, with its identity already assigned
    /// @return T the aggregate as it ended up stored
    T save(T aggregate);

    /// Removes an aggregate from the store for good.
    ///
    /// This is the hard deletion, and it leaves no audit trail behind. [#softDelete(Object)] is
    /// what an ordinary business deletion should use.
    ///
    /// @param id the identity
    void deleteById(ID id);

    /// Marks an aggregate as deleted without removing it from the store.
    ///
    /// It takes the whole aggregate rather than its identity because the caller already holds it:
    /// it had to read it to confirm the aggregate exists in the first place.
    ///
    /// @param aggregate the aggregate to take out of service
    void softDelete(T aggregate);

    /// Tells whether some aggregate already occupies that value in that field.
    ///
    /// It is the operation the `@Unique` annotation rests on. Each adapter resolves it with what
    /// its store knows how to do: Keycloak can only search a handful of fields, while JPA can do it
    /// for any of them with a derived query.
    ///
    /// @param field the field to check
    /// @param value the value being looked for
    /// @return boolean true when the value is already taken
    boolean existsByField(UniqueField field, String value);

}
