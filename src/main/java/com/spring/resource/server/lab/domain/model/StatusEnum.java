package com.spring.resource.server.lab.domain.model;

/// Lifecycle status of an [AuditableModel].
///
/// [#DELETED] is a soft deletion: the row stays in the store and its `deletedAt` / `deletedBy`
/// are recorded in [AuditInfo]. That pairing is why the status is declared on [AuditableModel]
/// and not on [BaseModel].
public enum StatusEnum {
    /// In use and visible to normal queries.
    ACTIVE,
    /// Kept but not in use; can be reactivated.
    INACTIVE,
    /// Soft-deleted; see [AuditInfo#deleted(String)].
    DELETED
}
