package com.spring.resource.server.lab.domain.identity;

import java.io.Serializable;

/// Strategy for models whose identity is decided outside the domain: a database sequence, an
/// `auto_increment` column, or an identity provider such as Keycloak that returns its own id when
/// the account is created.
///
/// Returning null is the whole point, not a gap. It is what lets a model be built before its
/// identity exists, and the outbound adapter fills it in afterwards through the model's
/// `withId(…)` copy method. Any model using this strategy therefore has a null id between being
/// created and being stored, which is exactly the window
/// [com.spring.resource.server.lab.domain.model.BaseModel#equals(Object)] falls back to reference
/// equality for.
///
/// The domain deliberately does not name *which* store assigns the identity. It knows only that
/// it is not the one deciding; whether the value comes from PostgreSQL or from Keycloak is a
/// question for the adapter, and naming either one here would drag a technology into the domain.
///
/// @param <ID> type of the identifier the store will assign
public final class StoreAssignedIdGenerator<ID extends Serializable> implements IdGenerator<ID> {

    /// @param modelType ignored; this strategy derives nothing from the model
    /// @return null, always — the store assigns the identity
    @Override
    public ID generateId(Class<?> modelType) {
        return null;
    }

}
