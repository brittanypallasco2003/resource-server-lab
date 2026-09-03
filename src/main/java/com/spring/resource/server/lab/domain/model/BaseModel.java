package com.spring.resource.server.lab.domain.model;

import java.io.Serializable;

import lombok.Getter;

/// Root of every domain model: the only thing they all share is having an identity.
///
/// The identifier is generic so it can hold simple keys (`String`, `Long`, a value object such as
/// [com.spring.resource.server.lab.domain.model.valueobject.UserId]) as well as composite ones;
/// the only requirement is that the key type is `Serializable`.
///
/// The hierarchy is sealed on purpose. There are exactly two ways of being a model: with an audit
/// trail ([AuditableModel]) or without one ([NonAuditableModel]). A third one would have to be
/// declared here explicitly instead of sneaking in through inheritance.
///
/// Neither the status nor the audit trail live here — see [AuditableModel] for why they belong
/// together one level down.
///
/// @param <ID> type of the identifier
@Getter
public abstract sealed class BaseModel<ID extends Serializable> permits AuditableModel, NonAuditableModel {

    private final ID id;

    /// @param id the identity assigned by the repository; `null` until the model is persisted
    protected BaseModel(ID id) {
        this.id = id;
    }

}
