package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import levelup42.novapay_backend_hex.domain.model.ApiClient;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.AuthRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.AuthResponse;
import levelup42.novapay_backend_hex.infrastructure.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock ApiClientRepositoryPort clientRepository;
    @Mock JwtTokenProvider tokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    
    @InjectMocks AuthController controller;

    private ApiClient newClient(String clientId, String hashedSecret, List<String> roles, boolean active) {
        return new ApiClient(
                "id",
                clientId,
                clientId + "@test.local",
                hashedSecret,
                roles,
                active,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                null
        );
    }

    @Test
    void getToken_credencialesValidas_retorna200ConToken() {
        ApiClient client = newClient("c1", "hashed", List.of("ROLE_INVOICER"), true);
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(tokenProvider.generate("c1", client.getRoles())).thenReturn("jwt.token.aqui");
        when(tokenProvider.getExpirationMs()).thenReturn(3600000L);

        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "secret123"));

        assertEquals(200, resp.getStatusCode().value());
        assertEquals("jwt.token.aqui", resp.getBody().accessToken());
    }

    @Test
    void getToken_clienteNoExiste_retorna401() {
        when(clientRepository.findByClientId("noexiste")).thenReturn(Optional.empty());
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("noexiste", "x"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void getToken_passwordIncorrecto_retorna401() {
        ApiClient client = newClient("c1", "hashed", List.of(), true);
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "wrong"));
        assertEquals(401, resp.getStatusCode().value());
    }

    @Test
    void getToken_clienteInactivo_retorna401() {
        ApiClient client = newClient("c1", "hashed", List.of(), false); // active=false
        when(clientRepository.findByClientId("c1")).thenReturn(Optional.of(client));
        ResponseEntity<AuthResponse> resp = controller.getToken(new AuthRequest("c1", "x"));
        assertEquals(401, resp.getStatusCode().value());
    }
}
