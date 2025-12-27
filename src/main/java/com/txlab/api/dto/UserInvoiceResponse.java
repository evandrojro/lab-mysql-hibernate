package com.txlab.api.dto;

public record UserInvoiceResponse(
        Long userId,
        Long invoiceId,
        String message
) {
}
