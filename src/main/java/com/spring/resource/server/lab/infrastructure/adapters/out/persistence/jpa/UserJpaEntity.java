package com.spring.resource.server.lab.infrastructure.adapters.out.persistence.jpa;

import java.util.LinkedHashSet;
import java.util.Set;

import com.spring.resource.server.lab.domain.model.StatusEnum;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/// Persistent representation of a user in PostgreSQL.
///
/// It is a class **separate** from the domain's
/// [com.spring.resource.server.lab.domain.model.User], not that model annotated with `@Entity`.
/// The reason is twofold: JPA demands a no-argument constructor and mutable fields, and above all,
/// annotating the domain with `jakarta.persistence` would put the technology exactly where the
/// architecture forbids it.
///
/// The price is a mapper; the gain is that the table schema can change without dragging the domain
/// along, and the other way round.
///
/// It extends [AuditableJpaEntity] with `String` as the identifier, the same type
/// `User extends AuditableModel<String>` fixes. The id is not generated here: it arrives already
/// set from the domain, which is why there is no `@GeneratedValue` anywhere in this hierarchy.
@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = UserJpaEntity.USERNAME_CONSTRAINT, columnNames = "username"))
@AttributeOverride(name = "id", column = @Column(name = "id", length = 40))
@Getter
@Setter
public class UserJpaEntity extends AuditableJpaEntity<String> {

    /// Name of the unique index on `username`, declared here so the adapter can recognise which
    /// constraint a database error came from instead of matching on the message text.
    ///
    /// It is the last line of defence for username uniqueness. `@Unique` on `CreateUserRequest`
    /// and the check in `UpdateUserService` both read before they write, so two concurrent
    /// requests can pass them both and still collide here.
    public static final String USERNAME_CONSTRAINT = "uk_users_username";

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String email;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "sso_id")
    private String ssoId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles = new LinkedHashSet<>();

    /// No-argument constructor required by JPA.
    protected UserJpaEntity() {
    }

    /// Full constructor, the one the mapper uses when translating from the domain.
    ///
    /// @param id the identifier already decided in the domain
    /// @param status the lifecycle status
    /// @param auditInfo the audit trail
    /// @param username the username
    /// @param email the e-mail address
    /// @param firstName the given name
    /// @param lastName the family name
    /// @param enabled whether the account is usable
    /// @param ssoId the identifier in the identity provider
    /// @param roles the assigned roles
    public UserJpaEntity(String id, StatusEnum status, AuditInfoEmbeddable auditInfo, String username,
            String email, String firstName, String lastName, boolean enabled, String ssoId, Set<String> roles) {
        super(id, status, auditInfo);
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.ssoId = ssoId;
        this.roles = roles == null ? new LinkedHashSet<>() : new LinkedHashSet<>(roles);
    }

}
