package com.spring.resource.server.lab.domain.model;

import java.io.Serializable;

import lombok.Getter;

/// A model that carries an audit trail and a lifecycle status.
///
/// The status lives here rather than in [BaseModel] because it only makes sense next to the audit
/// trail: a [StatusEnum#DELETED] is worth little unless it can also be said when the row was
/// deleted and by whom, which is what [AuditInfo] records. A [NonAuditableModel] has no way of
/// answering those questions, so it has no business carrying the status either.
///
/// This constructor does **not** compute the audit defaults. [AuditInfo] applies them in its own
/// compact constructor, the same way the domain value objects validate themselves, so an
/// `AuditInfo` can never exist half-filled regardless of who built it. All this constructor
/// decides is what to do when no trail is supplied at all.
///
/// @param <ID> type of the identifier
@Getter
public abstract non-sealed class AuditableModel<ID extends Serializable> extends BaseModel<ID> {

    private final AuditInfo auditInfo;
    private StatusEnum status;

    /// @param id the identity assigned by the repository; `null` until the model is persisted
    /// @param status the lifecycle status; defaults to [StatusEnum#ACTIVE] when missing
    /// @param auditInfo the audit trail; a fresh one is created when missing
    protected AuditableModel(ID id, StatusEnum status, AuditInfo auditInfo) {
        super(id);
        this.status = status != null ? status : StatusEnum.ACTIVE;
        this.auditInfo = auditInfo != null ? auditInfo : AuditInfo.created(null);
    }

}
