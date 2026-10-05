package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest;

import jakarta.validation.Valid;
import levelup42.novapay_backend_hex.domain.model.ApiClient;
import levelup42.novapay_backend_hex.domain.model.Company;
import levelup42.novapay_backend_hex.domain.model.enums.TaxAgency;
import levelup42.novapay_backend_hex.domain.model.valueObject.TaxId;
import levelup42.novapay_backend_hex.domain.port.out.ApiClientRepositoryPort;
import levelup42.novapay_backend_hex.domain.port.out.CompanyRepositoryPort;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.entity.PosTerminalEntity;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.repository.JpaPosTerminalRepository;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.VerifactuRegisterRequest;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.entity.VerifactuSubscriptionEntity;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.repository.JpaInvoiceRepository;
import levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.repository.JpaVerifactuSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registro Verifactu para frontend TPV.
 * Crea la empresa en tabla companies y un api_client asociado para auth legacy /auth/token.
 */
@RestController
@RequestMapping("/api/v1/verifactu")
@RequiredArgsConstructor
public class VerifactuRegistrationController {

    private static final BigDecimal OVERAGE_PER_INVOICE = new BigDecimal("0.05");
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{12,}$";

    private final CompanyRepositoryPort companyRepository;
    private final ApiClientRepositoryPort apiClientRepository;
    private final JpaVerifactuSubscriptionRepository subscriptionRepository;
    private final JpaInvoiceRepository invoiceRepository;
    private final JpaPosTerminalRepository posTerminalRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/company/{clientId}")
    public ResponseEntity<?> getCompanyByClientId(@PathVariable String clientId) {
        return apiClientRepository.findByClientId(clientId)
            .map(apiClient -> {
                if (apiClient.getLinkedCompanyId() == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "El cliente no tiene empresa vinculada"
                    ));
                }

                return companyRepository.findById(apiClient.getLinkedCompanyId())
                    .map(company -> {
                        Map<String, Object> companyPayload = new LinkedHashMap<>();
                        companyPayload.put("id", company.getId().toString());
                        companyPayload.put("name", company.getName());
                        companyPayload.put("taxId", company.getTaxId().getValue());
                        companyPayload.put("address", company.getAddress());

                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("clientId", apiClient.getClientId());
                        payload.put("company", companyPayload);

                        return ResponseEntity.ok(payload);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "No existe company para el cliente indicado"
                    )));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "No existe cliente API para clientId " + clientId
            )));
    }

    @GetMapping("/subscription/{clientId}")
    public ResponseEntity<?> getSubscriptionSummary(@PathVariable String clientId) {
        return apiClientRepository.findByClientId(clientId)
            .map(apiClient -> {
                if (apiClient.getLinkedCompanyId() == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "El cliente no tiene empresa vinculada"
                    ));
                }

                return subscriptionRepository.findByCompanyId(apiClient.getLinkedCompanyId())
                    .map(subscription -> {
                        OffsetDateTime now = OffsetDateTime.now();
                        OffsetDateTime periodEnd = subscription.getBillingCycle().equalsIgnoreCase("YEARLY")
                            ? subscription.getPeriodStart().plusYears(1)
                            : subscription.getPeriodStart().plusMonths(1);

                        int consumed = invoiceRepository
                            .findByCompanyIdAndIssueDateBetween(
                                subscription.getCompanyId(),
                                subscription.getPeriodStart().toLocalDate(),
                                now.toLocalDate()
                            )
                            .size();

                        String activeTerminalId = ensureTerminalForCompany(subscription.getCompanyId())
                            .getId()
                            .toString();

                        if (subscription.getCurrentPeriodInvoices() != consumed) {
                            subscription.setCurrentPeriodInvoices(consumed);
                            subscriptionRepository.save(subscription);
                        }

                        int included = subscription.getIncludedInvoices();
                        int overageInvoices = Math.max(0, consumed - included);
                        BigDecimal estimatedOverage = subscription.getOveragePerInvoice()
                            .multiply(BigDecimal.valueOf(overageInvoices));
                        BigDecimal estimatedTotal = subscription.getBaseAmount().add(estimatedOverage);
                        long serviceDaysRemaining = Math.max(
                            0,
                            ChronoUnit.DAYS.between(now.toLocalDate(), periodEnd.toLocalDate())
                        );

                        Map<String, Object> summary = new LinkedHashMap<>();
                        summary.put("clientId", apiClient.getClientId());
                        summary.put("companyId", subscription.getCompanyId().toString());
                        summary.put("terminalId", activeTerminalId);
                        summary.put("planCode", subscription.getPlanCode());
                        summary.put("billingCycle", subscription.getBillingCycle());
                        summary.put("periodStart", subscription.getPeriodStart().toString());
                        summary.put("periodEnd", periodEnd.toString());
                        summary.put("serviceDaysRemaining", serviceDaysRemaining);
                        summary.put("paymentStatus", subscription.getPaymentStatus());
                        summary.put("includedInvoices", included);
                        summary.put("consumedInvoices", consumed);
                        summary.put("remainingInvoices", Math.max(0, included - consumed));
                        summary.put("overageInvoices", overageInvoices);
                        summary.put("baseAmount", subscription.getBaseAmount().toPlainString());
                        summary.put("overagePerInvoice", subscription.getOveragePerInvoice().toPlainString());
                        summary.put("estimatedOverage", estimatedOverage.toPlainString());
                        summary.put("estimatedTotal", estimatedTotal.toPlainString());

                        return ResponseEntity.ok(summary);
                    })
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "error", "No existe suscripción para la empresa vinculada"
                    )));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "No existe cliente API para clientId " + clientId
            )));
        }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody VerifactuRegisterRequest request) {
        try {
            if (!request.isNewSystem() && (request.getClientHash() == null || request.getClientHash().isBlank())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "clientHash es obligatorio cuando isNewSystem=false"
                ));
            }

            if (!request.getPassword().equals(request.getPasswordConfirmation())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "La confirmación de contraseña no coincide"
                ));
            }

            if (!request.getPassword().matches(PASSWORD_REGEX)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "La contraseña debe tener al menos 12 caracteres, mayúscula, minúscula, número y símbolo"
                ));
            }

            PricingPlan pricingPlan = resolvePlan(request.getPlanCode(), request.getBillingCycle());

            if (apiClientRepository.findByClientId(request.getTaxId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Ya existe un cliente API para taxId " + request.getTaxId()
                ));
            }

            if (apiClientRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                        "error", "Ya existe un cliente API para email " + request.getEmail()
                ));
            }

            Company company = new Company(
                    null,
                    request.getCompanyName().trim(),
                    TaxId.of(request.getTaxId()),
                    request.getAddress().trim(),
                    TaxAgency.AEAT
            );
            Company savedCompany = companyRepository.save(company);

            String chainSeed = null;
            if (!request.isNewSystem() && request.getClientHash() != null && !request.getClientHash().isBlank()) {
                chainSeed = request.getClientHash().trim();
            }

            ApiClient client = new ApiClient(
                    null,
                    request.getTaxId().trim(),
                    request.getEmail().trim().toLowerCase(),
                    passwordEncoder.encode(request.getPassword()),
                    List.of("ROLE_INVOICER", "ROLE_FISCAL"),
                    true,
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                        chainSeed,
                    savedCompany.getId(),
                    OffsetDateTime.now(),
                    null
            );
            apiClientRepository.save(client);

            PosTerminalEntity createdTerminal = posTerminalRepository.save(
                PosTerminalEntity.builder()
                    .name("TPV Principal")
                    .serialNumber(generateUniqueTerminalSerial())
                    .active(true)
                    .company(levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.entity.CompanyEntity.builder()
                        .id(savedCompany.getId())
                        .build())
                    .build()
            );

            subscriptionRepository.save(VerifactuSubscriptionEntity.builder()
                    .companyId(savedCompany.getId())
                    .planCode(pricingPlan.planCode)
                    .billingCycle(pricingPlan.billingCycle)
                    .includedInvoices(pricingPlan.includedInvoices)
                    .baseAmount(pricingPlan.baseAmount)
                    .overagePerInvoice(OVERAGE_PER_INVOICE)
                    .currentPeriodInvoices(0)
                    .periodStart(OffsetDateTime.now())
                    .paymentStatus("AL_CORRIENTE")
                    .build());

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Registro Verifactu completado",
                    "companyId", savedCompany.getId().toString(),
                    "terminalId", createdTerminal.getId().toString(),
                    "clientId", request.getTaxId().trim(),
                    "email", request.getEmail().trim().toLowerCase(),
                    "planCode", pricingPlan.planCode,
                    "billingCycle", pricingPlan.billingCycle,
                    "baseAmount", pricingPlan.baseAmount.toPlainString(),
                    "invoiceLimit", String.valueOf(pricingPlan.includedInvoices),
                    "overagePerInvoice", OVERAGE_PER_INVOICE.toPlainString()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "Ya existe una empresa o cliente con esos datos"
            ));
        }
    }

    private PricingPlan resolvePlan(String planCodeRaw, String billingCycleRaw) {
        String planCode = planCodeRaw.trim().toUpperCase();
        String billingCycle = billingCycleRaw.trim().toUpperCase();

        if (!(billingCycle.equals("MONTHLY") || billingCycle.equals("YEARLY"))) {
            throw new IllegalArgumentException("billingCycle inválido. Usa MONTHLY o YEARLY");
        }

        if (planCode.equals("PLAN_5000")) {
            return new PricingPlan(
                    "PLAN_5000",
                    billingCycle,
                    5000,
                    billingCycle.equals("MONTHLY") ? new BigDecimal("12.00") : new BigDecimal("110.00")
            );
        }

        if (planCode.equals("PLAN_8000")) {
            return new PricingPlan(
                    "PLAN_8000",
                    billingCycle,
                    8000,
                    billingCycle.equals("MONTHLY") ? new BigDecimal("15.00") : new BigDecimal("165.00")
            );
        }

        throw new IllegalArgumentException("planCode inválido. Usa PLAN_5000 o PLAN_8000");
    }

    private String generateUniqueTerminalSerial() {
        String serial;
        do {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            serial = "TPV-" + suffix;
        } while (posTerminalRepository.existsBySerialNumber(serial));
        return serial;
    }

    private PosTerminalEntity ensureTerminalForCompany(UUID companyId) {
        return posTerminalRepository.findFirstByCompany_IdAndActiveTrue(companyId)
            .orElseGet(() -> posTerminalRepository.save(
                PosTerminalEntity.builder()
                    .name("TPV Principal")
                    .serialNumber(generateUniqueTerminalSerial())
                    .active(true)
                    .company(levelup42.novapay_backend_hex.infrastructure.adapter.out.persistence.entity.CompanyEntity.builder()
                        .id(companyId)
                        .build())
                    .build()
            ));
    }

    private record PricingPlan(String planCode, String billingCycle, int includedInvoices, BigDecimal baseAmount) {}
}
