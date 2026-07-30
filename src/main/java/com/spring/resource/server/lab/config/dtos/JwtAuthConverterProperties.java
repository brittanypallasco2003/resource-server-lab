package com.spring.resource.server.lab.config.dtos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "jwt.auth.converter")
public record JwtAuthConverterProperties(
        @NotBlank(message = "El principal attribute del token no puede estar vacío") String principalAttribute,
        @NotBlank(message = "El id del resource no puede estar vacío") String resourceId
    ) {

}
