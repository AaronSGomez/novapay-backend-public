package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthRequest(
    @JsonProperty("clientId") String clientId, 
    @JsonProperty("clientSecret") String clientSecret
) {}
