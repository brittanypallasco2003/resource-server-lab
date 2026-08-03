package com.spring.resource.server.lab.controller.dto.Keycloak;

import java.util.Set;

import lombok.Builder;

@Builder
public record UserDto(String username, String email, String lastName, String firstName, Set<String> roles, String password) {

}
