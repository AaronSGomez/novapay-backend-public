package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.AuthRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.entity.ApiClientEntity;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.repository.JpaApiClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JpaApiClientRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldReturnJwtTokenForValidCredentials() {
        // Enforce the known secret by saving it before the test
        ApiClientEntity entity = repository.findByClientId("novapay-client").orElseThrow();
        entity.setHashedSecret(passwordEncoder.encode("known-secret-2024"));
        repository.save(entity);

        AuthRequest request = new AuthRequest("novapay-client", "known-secret-2024");

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/token", request, Map.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accessToken");
        assertThat(response.getBody().get("accessToken")).isNotNull();
    }

    @Test
    void shouldReturnUnauthorizedForInvalidCredentials() {
        AuthRequest request = new AuthRequest("novapay-client", "wrong-secret");

        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/auth/token", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
