package levelup42.novapay_backend_hex.domain.port.in;

import java.util.List;

public interface FiscalListInteractionsUseCase {
    List<FiscalInteractionResult> listInteractions(int limit);
    List<FiscalInteractionResult> listInteractionsForClient(String clientId, int limit);
}