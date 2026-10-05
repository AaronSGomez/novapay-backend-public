package levelup42.novapay_backend_hex.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI novapayOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NovaPay Fiscal API")
                .description("API REST para gestión de facturas electrónicas VERIFACTU / TicketBAI")
                .version("1.0.0")
                .contact(new Contact().name("LevelUp42").email("dev@levelup42.es")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Obtén el token con POST /api/v1/auth/token")));
    }
}
