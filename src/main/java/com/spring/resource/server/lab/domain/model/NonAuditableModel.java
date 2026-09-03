package com.spring.resource.server.lab.domain.model;

import java.io.Serializable;

/// A model with no audit trail: catalogues, lookup tables and anything else where knowing who
/// created a row and when adds nothing.
///
/// It deliberately adds no state of its own — not even a lifecycle [StatusEnum]. It exists so a
/// model can declare "I have an identity and nothing else" without dragging along the
/// [AuditInfo] fields it would never fill in.
///
/// @param <ID> type of the identifier
public abstract non-sealed class NonAuditableModel<ID extends Serializable> extends BaseModel<ID> {

    /// @param id the identity assigned by the repository
    protected NonAuditableModel(ID id) {
        super(id);
    }

}
