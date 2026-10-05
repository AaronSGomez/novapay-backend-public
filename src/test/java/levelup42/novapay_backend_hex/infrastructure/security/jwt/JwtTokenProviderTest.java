package levelup42.novapay_backend_hex.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String SECRET = "test-secret-must-be-32-chars-long!";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, 60L);
    }

    @Test
    void generate_devuelveTokenNoNulo() {
        String token = provider.generate("client-1", List.of("ROLE_INVOICER"));
        assertNotNull(token);
        assertTrue(token.startsWith("ey")); // JWT válido
    }

    @Test
    void validateAndGetClaims_tokenValido_devuelveClaims() {
        String token = provider.generate("client-42", List.of("ROLE_FISCAL"));
        Claims claims = provider.validateAndGetClaims(token);
        assertEquals("client-42", claims.getSubject());
    }

    @Test
    void validateAndGetClaims_tokenManipulado_lanzaJwtException() {
        String token = provider.generate("client-1", List.of()) + "MANIPULADO";
        assertThrows(JwtException.class, () -> provider.validateAndGetClaims(token));
    }

    @Test
    void validateAndGetClaims_tokenExpirado_lanzaJwtException() {
        // TTL de 0 minutos → expira inmediatamente
        JwtTokenProvider shortProvider = new JwtTokenProvider(SECRET, 0L);
        String token = shortProvider.generate("client-1", List.of());
        assertThrows(JwtException.class, () -> shortProvider.validateAndGetClaims(token));
    }

    @Test
    void generate_conRolesMultiples_rolesEnClaims() {
        List<String> roles = List.of("ROLE_INVOICER", "ROLE_FISCAL", "ROLE_ADMIN");
        String token = provider.generate("admin", roles);
        Claims claims = provider.validateAndGetClaims(token);
        List<?> rolesEnToken = claims.get("roles", List.class);
        assertEquals(3, rolesEnToken.size());
    }
}
