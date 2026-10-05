package levelup42.novapay_backend_hex.infrastructure.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro Spring Security que extrae y valida el Bearer JWT de cada petición.
 * Si el token es válido carga el SecurityContext para que los endpoints protegidos funcionen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = tokenProvider.validateAndGetClaims(token);
            String clientId = claims.getSubject();

            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(clientId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("JWT autenticado para cliente: {}", clientId);

        } catch (JwtException e) {
            log.warn("JWT inválido o expirado: {}", e.getMessage());
            // No lanzamos excepción — Spring Security rechazará la petición por no tener autenticación
        }

        filterChain.doFilter(request, response);
    }
}
