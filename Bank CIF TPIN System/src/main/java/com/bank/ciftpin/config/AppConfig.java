package com.bank.ciftpin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OpenAPI bankCifTpinOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Bank CIF & TPIN Management API")
                        .description("""
                                REST API for managing Bank Customer Information File (CIF) and Transaction PIN (TPIN).
                                
                                ## Features:
                                - **Register**: Create a new CIF account
                                - **Set TPIN**: Set a TPIN for a newly registered CIF
                                - **Authenticate**: Authenticate using CIF + TPIN → returns JWT token
                                - **Reset TPIN**: Unblock CIF and set a new TPIN
                                
                                ## Authentication Flow:
                                1. Call `/authenticate` with CIF + TPIN
                                2. Copy the `token` from the response
                                3. Click **Authorize** above and paste: `Bearer <token>`
                                
                                ## CIF Status Flow:
                                `PENDING_TPIN` → (Set TPIN) → `ACTIVE` → (3 failed auth) → `BLOCKED` → (Reset TPIN) → `ACTIVE`
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ahmed Nawar")
                                .email("ahmed.nawar@bank.com"))
                        .license(new License()
                                .name("MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter the JWT token returned by the /authenticate endpoint")));
    }
}