package com.spring.resource.server.lab.domain.repository;

import com.spring.resource.server.lab.domain.model.User;

/// Outbound port for the system that actually authenticates people.
///
/// It is separate from [UserRepository] because the two answer different questions. The
/// repository stores what this application knows about a user; the identity provider owns the
/// account that lets that user log in. They can live in different systems, fail independently,
/// and only one of them ever sees a password.
///
/// The domain does not name the provider. Whether the account ends up in Keycloak, in an LDAP
/// directory or somewhere else is a decision for the adapter; from here it is just "the place
/// that hands back an identifier".
public interface IdentityProvider {

    /// Registers an account for a user that has just been built by the domain, and returns the
    /// identifier the provider assigned to it.
    ///
    /// The returned value belongs in the user's `ssoId`, never in its `id`: the domain
    /// identifier was already decided before this call.
    ///
    /// The password travels as a separate parameter for the same reason [User] does not carry
    /// one: it is a provisioning detail, not an attribute of the user.
    ///
    /// @param user the user to register; its identity is already assigned
    /// @param rawPassword the initial password, in clear text
    /// @return the identifier assigned by the provider
    String register(User user, String rawPassword);

    /// Brings the account in line with the user's current data.
    ///
    /// Only what the provider actually needs travels here — the user name, the e-mail, whether the
    /// account is usable, and the roles. A given name or a family name is this application's data,
    /// not the provider's, and sending it would turn every profile edit into a call across the
    /// network for nothing.
    ///
    /// It takes the whole [User] rather than a bag of fields so the port does not have to be
    /// rewritten every time the provider starts caring about one more attribute; the adapter picks
    /// what it needs.
    ///
    /// @param ssoId the identifier the provider assigned when the account was registered
    /// @param user the user in its current state
    void updateAccount(String ssoId, User user);

    /// Replaces the password of an account that already exists.
    ///
    /// It lives here, and no longer in a port of its own, because there is only one system that
    /// ever holds a password and it is this one. A second port pointing at the same provider only
    /// raised the question of which of the two to call.
    ///
    /// @param ssoId the identifier the provider assigned when the account was registered
    /// @param rawPassword the new password, in clear text
    void changePassword(String ssoId, String rawPassword);

    /// Removes an account from the provider.
    ///
    /// It exists so a caller can undo a registration whose follow-up steps failed. An
    /// implementation must not fail when the account is already gone: undoing something twice
    /// should be as harmless as undoing it once.
    ///
    /// @param ssoId the identifier the provider assigned when the account was registered
    void unregister(String ssoId);

}
