package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/// Las seis columnas de auditoría, agrupadas para poder incrustarlas en cualquier tabla.
///
/// Es una clase aparte de [com.spring.resource.server.lab.domain.model.AuditInfo] por el mismo
/// motivo por el que [UserJpaEntity] es una clase aparte de
/// [com.spring.resource.server.lab.domain.model.User]: el record del dominio no puede llevar
/// anotaciones de `jakarta.persistence`, y JPA necesita constructor sin argumentos y campos
/// mutables, que un record no da.
///
/// Aquí no se ponen valores por defecto: cuando estos datos llegan, el dominio ya los rellenó.
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
