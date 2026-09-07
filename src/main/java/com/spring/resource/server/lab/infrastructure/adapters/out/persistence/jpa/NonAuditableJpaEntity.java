package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.io.Serializable;

import jakarta.persistence.MappedSuperclass;

/// Entity with no audit columns, the mirror of
/// [com.spring.resource.server.lab.domain.model.NonAuditableModel].
///
/// It adds nothing: it exists so a lookup table can declare "I only have a key" without dragging
/// along the six columns of [AuditInfoEmbeddable] nor the status one.
///
/// @param <ID> type of the identifier
@MappedSuperclass
public abstract class NonAuditableJpaEntity<ID extends Serializable> extends BaseJpaEntity<ID> {

    /// No-argument constructor required by JPA.
    protected NonAuditableJpaEntity() {
    }

    /// @param id the identifier already decided by the domain
    protected NonAuditableJpaEntity(ID id) {
        super(id);
    }

}
