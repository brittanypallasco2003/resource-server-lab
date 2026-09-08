package com.spring.resource.server.lab.domain.identity;

import java.io.Serializable;

/// Strategy for deciding the identity of a model before it is stored.
///
/// It exists because a generic type does not survive into the running program: nothing inside
/// [com.spring.resource.server.lab.domain.model.BaseModel] can ask "was my `ID` a `String` or a
/// `Long`?" and branch on the answer. The choice therefore has to be made where the type is still
/// written down — in the concrete model — and each model names the implementation it wants.
///
/// The interface is generic for the same reason `BaseModel` is: an identity may be a simple key,
/// a value object, or a composite one, and the only thing every implementation has to promise is
/// that the key can be serialized.
///
/// @param <ID> type of the identifier this strategy produces
public interface IdGenerator<ID extends Serializable> {

    /// Produces the identity for a model that does not exist in its store yet.
    ///
    /// @param modelType the concrete class being created, so an implementation can derive part of
    ///        the identity from it — a prefix, a discriminator, a table name
    /// @return the new identity, or null when the store is the one that assigns it
    ID generateId(Class<?> modelType);

}
