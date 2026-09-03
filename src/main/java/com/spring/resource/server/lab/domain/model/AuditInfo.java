package com.spring.resource.server.lab.domain.model;

import java.time.OffsetDateTime;

/// Audit trail of an [AuditableModel].
///
/// The defaults are applied in the compact constructor rather than in a static factory, because
/// that is what makes them unavoidable. A record always exposes its canonical constructor, so a
/// factory only protects the callers who remember to use it — and the callers that matter here
/// are the outbound mappers, which rebuild models straight from a row or a provider
/// representation with `new`, and the copy methods below, since a record has no `with`. Putting
/// the defaults here means every one of those paths goes through them.
///
/// `OffsetDateTime.now()` is read once and shared, so a freshly created trail has
/// `createdAt.equals(updatedAt)` — a reliable way of saying "never modified".
///
/// The cost of the invariant is that a deliberately unknown `createdAt` can no longer be
/// represented; legacy rows without a creation date have to be given one.
///
/// @param createdAt when the row was created; defaults to now
/// @param updatedAt when the row was last modified; defaults to now
/// @param deletedAt when the row was soft-deleted; null while it is still alive
/// @param createdBy author of the creation
/// @param updatedBy author of the last modification
/// @param deletedBy author of the soft deletion
public record AuditInfo(
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime deletedAt,
        String createdBy,
        String updatedBy,
        String deletedBy) {

    public AuditInfo {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = createdAt != null ? createdAt : now;
        updatedAt = updatedAt != null ? updatedAt : now;
    }

    /// Trail of a row that has just been created.
    ///
    /// The constructor already guarantees the dates; this factory exists to name the intent and
    /// to spare the caller a line of six mostly-null arguments.
    ///
    /// @param createdBy author of the creation; may be null when there is no user in context
    /// @return AuditInfo with creation and modification stamped at the current instant
    public static AuditInfo created(String createdBy) {
        return new AuditInfo(null, null, null, createdBy, createdBy, null);
    }

    /// Copy of the trail with the modification recorded at the current instant.
    /// @param updatedBy author of the modification
    /// @return AuditInfo a copy with a refreshed `updatedAt`
    public AuditInfo modified(String updatedBy) {
        return new AuditInfo(createdAt, OffsetDateTime.now(), deletedAt, createdBy, updatedBy, deletedBy);
    }

    /// Copy of the trail with the soft deletion recorded at the current instant.
    ///
    /// `updatedAt` moves along with `deletedAt`: a soft delete is also a modification of the row.
    ///
    /// @param deletedBy author of the deletion
    /// @return AuditInfo a copy with `deletedAt` and `deletedBy` set
    public AuditInfo deleted(String deletedBy) {
        OffsetDateTime now = OffsetDateTime.now();
        return new AuditInfo(createdAt, now, now, createdBy, updatedBy, deletedBy);
    }

}
