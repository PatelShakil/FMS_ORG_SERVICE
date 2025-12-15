package com.shakilpatel.org.service.config;
import jakarta.ws.rs.ApplicationPath;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;

@SuppressWarnings({"EmptyClass", "SuppressionAnnotation"})
@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT")
@SecurityScheme(
        securitySchemeName = "jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
@OpenAPIDefinition(
        info = @Info(
                title = "BoloIndia Organization Service API",
                version = "1.0",
                description = "Organization Management Service",
                contact = @Contact(
                        name = "MuhammadShakil Patel",
                        email = "shakil@boloindia.in"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8086/org-service", description = "Development")
        },
        security = {
                @SecurityRequirement(name = "jwt")
        }
)
public class BootStrap extends jakarta.ws.rs.core.Application {
    
}

//mkdir -p service-service/src/main/java/com/shakilpatel/service/{entity,ejb,rest,dto,kafka,config}
//mkdir -p service-service/src/main/resources/META-INF
//mkdir -p service-service/src/main/webapp/WEB-INF
