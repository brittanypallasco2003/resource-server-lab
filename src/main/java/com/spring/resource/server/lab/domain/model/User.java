package com.spring.resource.server.lab.domain.model;

import java.util.Set;

import com.spring.resource.server.lab.domain.model.valueobject.Email;
import com.spring.resource.server.lab.domain.identity.IdGenerator;
import com.spring.resource.server.lab.domain.identity.PrefixedUuidGenerator;
import com.spring.resource.server.lab.domain.validation.DomainValidator;

import lombok.Getter;

/// A user of the system, expressed without any technological dependency.
///
/// It is a class and no longer a record because it extends [AuditableModel]: a record cannot
/// extend anything. What is gained in exchange is the identity, the lifecycle status and the
/// audit trail, which no longer have to be declared here.
///
/// It deliberately carries no password: a credential is not an attribute of a user, it is a
/// provisioning act. That is why it travels through
/// [com.spring.resource.server.lab.domain.repository.IdentityProvider] instead, which is the
/// only port that ever sees one.
///
/// Every instance is immutable: the copy methods below return a new user rather than mutating
/// this one, which is what keeps the validation in the constructor meaningful.
@Getter
public class User extends AuditableModel<String> {

    /// Role granted when a sign-up requests none. It used to be buried in the Keycloak adapter;
    /// it is a business rule, so its place is the domain.
    ///
    /// Uppercase, like every other role: see [DomainValidator#normalizeSet(java.util.Set, String)].
    public static final String DEFAULT_ROLE = "USER";

    private static final String USERNAME_FIELD = "El nombre de usuario";
    private static final String EMAIL_FIELD = "El correo electrónico";

    /// Identity strategy for users. It is named here, and not resolved inside `BaseModel`, because
    /// this is the last place where `<String>` is still written down; see [IdGenerator].
    private static final IdGenerator<String> ID_GENERATOR = new PrefixedUuidGenerator();

    private final String username;
    private final Email email;
    private final String firstName;
    private final String lastName;
    private final Set<String> roles;
    private final boolean enabled;
    private final String ssoId;

    /// Full constructor, also the reconstruction path used by the outbound mappers: it is the
    /// only way to rebuild a user with the status and the audit trail a stored row already has.
    ///
    /// The validation lives here, and not in the use cases, for the same reason it lives in the
    /// compact constructor of [Email]: as long as this is the only door, an invalid `User` cannot
    /// be built anywhere in the codebase.
    ///
    /// @param id identity assigned by the repository; null while the user is not persisted
    /// @param status lifecycle status; defaults to [StatusEnum#ACTIVE]
    /// @param auditInfo audit trail; a fresh one is created when null
    /// @param username username, unique within the realm
    /// @param email validated e-mail address
    /// @param firstName given name
    /// @param lastName family name
    /// @param roles assigned roles; never empty, see [#DEFAULT_ROLE]
    /// @param enabled whether the account is usable
    /// @param ssoId identifier of this user in the identity provider, when it differs from the id
    /// @throws com.spring.resource.server.lab.domain.exception.InvalidUserDataException if the
    ///         username is blank or the e-mail is missing
    public User(String id, StatusEnum status, AuditInfo auditInfo, String username, Email email,
            String firstName, String lastName, Set<String> roles, boolean enabled, String ssoId) {
        super(id, status, auditInfo);
        this.username = DomainValidator.requireNotBlank(username, USERNAME_FIELD);
        this.email = DomainValidator.requireNotNull(email, EMAIL_FIELD);
        this.firstName = firstName;
        this.lastName = lastName;
        this.roles = DomainValidator.normalizeSet(roles, DEFAULT_ROLE);
        this.enabled = enabled;
        this.ssoId = ssoId;
    }


    /// Builds a user that does not exist in the repository yet, with no identity assigned.
    ///
    /// @param username user name
    /// @param email e-mail address
    /// @param firstName given name
    /// @param lastName family name
    /// @param roles requested roles; [#DEFAULT_ROLE] applies when null or empty
    /// @return User ready to be created, active and with a fresh audit trail
    public static User created(String username, Email email, String firstName, String lastName, Set<String> roles) {
        return new User(ID_GENERATOR.generateId(User.class), StatusEnum.ACTIVE, AuditInfo.created(null),
                username, email, firstName, lastName, roles, true, null);
    }

    /// Returns a copy carrying the identity. Adapters call it right after creating the user in
    /// the target store, which is when the identifier becomes known.
    ///
    /// @param newId the freshly assigned identity
    /// @return User a copy with an identity; everything else is preserved
    public User withId(String newId) {
        return new User(newId, getStatus(), getAuditInfo(),
                username, email, firstName, lastName, roles, enabled, ssoId);
    }

    /// Returns a copy carrying the identifier this user has in the identity provider.
    ///
    /// It is set after the account has been registered, which is the only moment the provider's
    /// identifier becomes known. It is kept apart from [BaseModel#getId()] on purpose: the domain
    /// identifier is generated here, before anything leaves the application, and stays stable even
    /// if the provider is replaced. `ssoId` is a foreign key into somebody else's system.
    ///
    /// @param newSsoId the identifier assigned by the identity provider
    /// @return User a copy with the provider's identifier; everything else is preserved
    public User withSsoId(String newSsoId) {
        return new User(getId(), getStatus(), getAuditInfo(),
                username, email, firstName, lastName, roles, enabled, newSsoId);
    }

    /// Returns a copy with the editable data replaced and the modification stamped on the audit
    /// trail.
    ///
    /// Identity, status, `ssoId` and `enabled` are preserved: an update of the profile is not a
    /// reactivation nor a change of provider. Those are separate operations and deserve their own
    /// methods when they are needed.
    ///
    /// @param username the new user name
    /// @param email the new e-mail address
    /// @param firstName the new given name
    /// @param lastName the new family name
    /// @param roles the new roles
    /// @return User a copy with the new data and a refreshed `updatedAt`
    public User withData(String username, Email email, String firstName, String lastName, Set<String> roles) {
        return new User(getId(), getStatus(), getAuditInfo().modified(),
                username, email, firstName, lastName, roles, enabled, ssoId);
    }


    /// Returns a copy in a different lifecycle state.
    ///
    /// The audit trail is not stamped here: a change of state is not the same event as a change of
    /// data, and a soft deletion records itself through [AuditInfo#deleted(String)]. Whoever
    /// performs the transition decides which of the two applies.
    ///
    /// @param newStatus the new lifecycle status
    /// @return User a copy in the new state; everything else is preserved
    public User withStatus(StatusEnum newStatus) {
        return new User(getId(), newStatus, getAuditInfo(),
                username, email, firstName, lastName, roles, enabled, ssoId);
    }

}
