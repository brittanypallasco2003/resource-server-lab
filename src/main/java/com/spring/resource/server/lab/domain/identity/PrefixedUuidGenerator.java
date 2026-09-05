package com.spring.resource.server.lab.domain.identity;

import java.util.UUID;

/// Produces text identities of the form `USE-3f9a2c…`: three letters taken from the model's own
/// name, a dash, and a random UUID with its dashes stripped.
///
/// The prefix buys readability, not uniqueness — the UUID already guarantees that. An identifier
/// found in a log line or a support ticket says what it belongs to without anyone having to look
/// it up.
///
/// It carries no state, so a single instance can be shared by every model that wants this
/// strategy.
public final class PrefixedUuidGenerator implements IdGenerator<String> {

    private static final int PREFIX_LENGTH = 3;

    @Override
    public String generateId(Class<?> modelType) {
        String name = modelType.getSimpleName().toUpperCase();
        String prefix = name.length() > PREFIX_LENGTH ? name.substring(0, PREFIX_LENGTH) : name;
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "%s-%s".formatted(prefix, uuid);
    }

}
