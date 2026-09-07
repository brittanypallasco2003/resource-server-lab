package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// The six audit columns, grouped so they can be embedded in any table.
///
/// It is a class separate from [com.spring.resource.server.lab.domain.model.AuditInfo] for the same
/// reason [UserJpaEntity] is separate from
/// [com.spring.resource.server.lab.domain.model.User]: the domain record cannot carry
/// `jakarta.persistence` annotations, and JPA needs a no-argument constructor and mutable fields,
/// which a record does not give.
///
/// No defaults are applied here: by the time this data arrives, the domain has already filled it
/// in.
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class AuditInfoEmbeddable {

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "deleted_by")
    private String deletedBy;


}
