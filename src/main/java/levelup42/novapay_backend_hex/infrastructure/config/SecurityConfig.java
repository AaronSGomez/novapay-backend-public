package levelup42.novapay_backend_hex.infrastructure.config;

import levelup42.novapay_backend_hex.infrastructure.security.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CONFIGURAR CORS: Para que tu dominio duckdns sea bienvenido
            .cors(cors -> cors.configurationSource(request -> {
                var corsConfiguration = new CorsConfiguration();
                corsConfiguration.setAllowedOrigins(List.of(
                        "https://novapay.duckdns.org",
                        "http://localhost:3000",  // Para desarrollo local con Flutter Web
                        "http://localhost:8080"   // Para desarrollo local
                ));
                corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                corsConfiguration.setAllowedHeaders(List.of("*"));
                corsConfiguration.setAllowCredentials(true);
                return corsConfiguration;
            }))
            // 2. DESACTIVAR CSRF: Para APIs REST no es necesario
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Raíz — health check sin errores
                .requestMatchers("/").permitAll()
                // Endpoints públicos de autenticación/registro
                .requestMatchers(
                    "/api/v1/auth/token",
                    "/api/v1/auth/registro",
                    "/api/v1/auth/login",
                    "/api/v1/auth/verify-email",
                    "/api/v1/auth/forgot-password",
                    "/api/v1/verifactu/register",
                    "/api/v1/verifactu/subscription/**"
                ).permitAll()
                // Actuator health — sin autenticación
                .requestMatchers("/actuator/health").permitAll()
                // Swagger UI & OpenAPI docs — público
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Todo lo demás requiere JWT válido
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * PasswordEncoder BCrypt — usado en AuthController para verificar clientSecret.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
