package com.spring.resource.server.lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.spring.resource.server.lab.infrastructure.config.properties.JwtAuthConverterProperties;
import com.spring.resource.server.lab.infrastructure.config.properties.KeycloakAdminProperties;

@EnableConfigurationProperties({JwtAuthConverterProperties.class, KeycloakAdminProperties.class})
@SpringBootApplication
public class ResourceServerLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceServerLabApplication.class, args);
	}

}
