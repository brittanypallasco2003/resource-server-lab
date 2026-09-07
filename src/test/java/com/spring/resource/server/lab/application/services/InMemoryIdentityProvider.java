package com.spring.resource.server.lab.application.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.spring.resource.server.lab.domain.model.User;
import com.spring.resource.server.lab.domain.repository.IdentityProvider;

/// In-memory implementation of [IdentityProvider] for the application-layer tests.
///
/// It imitates the only thing about the real provider that matters here: that it assigns an
/// identifier of its own, different from the one the domain generated. That is why it returns a
/// UUID with no prefix, which cannot be confused with the `USE-...` that `PrefixedUuidGenerator`
/// produces.
class InMemoryIdentityProvider implements IdentityProvider {

    /// Live accounts, indexed by the identifier this double assigned.
    private final Map<String, String> accounts = new LinkedHashMap<>();

    /// Counters used to check that the service calls the provider when it should and only then.
    int registerCalls = 0;
    int unregisterCalls = 0;
    int changePasswordCalls = 0;
    int updateAccountCalls = 0;

    /// Failure [#register] will throw on the next call, when not null.
    RuntimeException failureToThrow;

    @Override
    public String register(User user, String rawPassword) {
        registerCalls++;
        if (failureToThrow != null) {
            throw failureToThrow;
        }
        String ssoId = UUID.randomUUID().toString();
        accounts.put(ssoId, rawPassword);
        return ssoId;
    }

    @Override
    public void updateAccount(String ssoId, User user) {
        updateAccountCalls++;
    }

    @Override
    public void changePassword(String ssoId, String rawPassword) {
        changePasswordCalls++;
        accounts.put(ssoId, rawPassword);
    }

    @Override
    public void unregister(String ssoId) {
        unregisterCalls++;
        accounts.remove(ssoId);
    }

    /// @param ssoId the identifier returned by [#register]
    /// @return boolean `true` when the account is still registered
    boolean hasAccount(String ssoId) {
        return accounts.containsKey(ssoId);
    }

    /// @param ssoId the account's identifier
    /// @return String the password recorded for that account, or null when there is none
    String passwordOf(String ssoId) {
        return accounts.get(ssoId);
    }

}
