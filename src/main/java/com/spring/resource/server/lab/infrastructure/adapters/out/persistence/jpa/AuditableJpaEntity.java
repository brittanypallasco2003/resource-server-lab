package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.io.Serializable;

import com.spring.resource.server.lab.domain.model.StatusEnum;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/// Entity with a lifecycle status and an audit trail, the mirror of
/// [com.spring.resource.server.lab.domain.model.AuditableModel].
///
/// The status is stored as text (`EnumType.STRING`) and not as a number. With `ORDINAL`, inserting
/// a new value in the middle of the enum would silently reinterpret every row already written.
///
/// The enum being persisted is the domain's own: an entity may depend on the domain, that is the
/// allowed direction. The other way round is what is forbidden.
///
/// @param <ID> type of the identifier
@Getter
@Setter
@MappedSuperclass
public abstract class AuditableJpaEntity<ID extends Serializable> extends BaseJpaEntity<ID> {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnum status;

    @Embedded
    private AuditInfoEmbeddable auditInfo;

    /// No-argument constructor required by JPA.
    protected AuditableJpaEntity() {
    }

    /// @param id the identifier already decided by the domain
    /// @param status the lifecycle status
    /// @param auditInfo the audit trail
    protected AuditableJpaEntity(ID id, StatusEnum status, AuditInfoEmbeddable auditInfo) {
        super(id);
        this.status = status;
        this.auditInfo = auditInfo;
    }

}
