package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.io.Serializable;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Transient;
import lombok.Getter;

/// Raíz de las entidades JPA, espejo de
/// [com.spring.resource.server.lab.domain.model.BaseModel] en el lado de la persistencia.
///
/// Es `@MappedSuperclass` y no `@Entity`: no le corresponde ninguna tabla propia, solo aporta la
/// columna del identificador a las tablas de quienes la extienden. Sin esa anotación, JPA ni
/// siquiera vería el campo `id` y arrancaría quejándose de que la entidad no tiene identificador.
///
/// El identificador es genérico igual que en el dominio. Hibernate resuelve de qué tipo es
/// mirando el argumento que fija la entidad concreta (`UserJpaEntity extends
/// BaseJpaEntity<String>`), así que basta con declararlo una vez aquí.
///
/// **No lleva `@GeneratedValue` a propósito.** El identificador se decide en el dominio antes de
/// guardar y llega hecho a través del mapper; la base de datos solo lo escribe. Ese es también el
/// motivo de implementar [Persistable]: ver [#isNew()].
///
/// @param <ID> tipo del identificador
@Getter
@MappedSuperclass
public abstract class BaseJpaEntity<ID extends Serializable> implements Persistable<ID> {

    @Id
    private ID id;

    /// Marca de «todavía no está en la tabla». Es `@Transient`, así que no existe como columna:
    /// vive solo mientras el objeto está en memoria.
    @Transient
    private boolean pendingInsert = true;

    /// Constructor sin argumentos exigido por JPA.
    protected BaseJpaEntity() {
    }

    /// @param id el identificador ya decidido por el dominio
    protected BaseJpaEntity(ID id) {
        this.id = id;
    }

    /// Indica a Spring Data si la entidad es un alta o una modificación.
    ///
    /// Sin esto, Spring Data decide mirando si el id es nulo. Como aquí el id **siempre** viene
    /// puesto, daría siempre por hecho que la fila ya existe: cada alta acabaría en un `merge`,
    /// que lanza un `SELECT` para comprobarlo antes de poder hacer el `INSERT`. Con la marca, un
    /// alta es un `INSERT` directo.
    @Override
    public boolean isNew() {
        return pendingInsert;
    }

    /// Baja la marca en cuanto la fila existe de verdad: al insertarla o al leerla de la tabla.
    @PostLoad
    @PrePersist
    void markAsStored() {
        this.pendingInsert = false;
    }

    /// Dos entidades son la misma si comparten identificador, igual que en el dominio.
    ///
    /// La comparación es por `instanceof` y no por `getClass()` porque Hibernate entrega a veces
    /// un proxy —una subclase invisible generada al vuelo— y con `getClass()` una entidad nunca
    /// sería igual a su propio proxy.
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BaseJpaEntity<?> other) || id == null) {
            return false;
        }
        return id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

}
