package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {
    
    @GetMapping("/")
    public String healthCheck() {
        return "Novapay API is UP and Running!";
    }
}
