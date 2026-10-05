package levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.mapper;

import levelup42.novapay_backend_hex.domain.model.valueObject.Money;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCancelCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceCreateCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceLineCommand;
import levelup42.novapay_backend_hex.domain.port.in.InvoiceResult;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceCancelRequestDTO;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceLineRequestDTO;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceRequestDTO;
import levelup42.novapay_backend_hex.infrastructure.adapter.in.rest.dto.InvoiceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface WebMapper {

    @Mapping(target = "lines", source = "lines")
    @Mapping(target = "rectifiedInvoiceId", source = "rectifiedInvoiceId")
    InvoiceCreateCommand toCommand(InvoiceRequestDTO dto);

    @Mapping(target = "unitPrice", source = "unitPrice", qualifiedByName = "bigDecimalToMoney")
    InvoiceLineCommand toCommand(InvoiceLineRequestDTO dto);

    InvoiceCancelCommand toCommand(InvoiceCancelRequestDTO dto);

    @Mapping(target = "series", expression = "java(result.invoiceNumber().getSeries())")
    @Mapping(target = "number", expression = "java(result.invoiceNumber().getNumber())")
    @Mapping(target = "totalAmount", source = "totalAmount", qualifiedByName = "moneyToBigDecimal")
    InvoiceResponse toResponse(InvoiceResult result);

    @Named("bigDecimalToMoney")
    default Money bigDecimalToMoney(BigDecimal value) {
        return value != null ? Money.of(value) : null;
    }

    @Named("moneyToBigDecimal")
    default BigDecimal moneyToBigDecimal(Money money) {
        return money != null ? money.getValue() : null;
    }
}

