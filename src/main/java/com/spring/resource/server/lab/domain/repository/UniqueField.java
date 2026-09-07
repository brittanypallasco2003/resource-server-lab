package com.spring.resource.server.lab.domain.repository;

/// Fields a repository knows how to check for uniqueness.
///
/// It is an enum and not a `String` on purpose: `@Unique(field = "usernme")` with a typo would be
/// a runtime failure, whereas a non-existent enum value does not compile. The price is that adding
/// a queryable field forces a change here and in every adapter — which is exactly the warning that
/// matters, because not every store knows how to search by any field.
public enum UniqueField {

    /// Username.
    USERNAME,

    /// E-mail address.
    EMAIL

}
