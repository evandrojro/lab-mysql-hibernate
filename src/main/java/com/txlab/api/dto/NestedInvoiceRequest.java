package com.txlab.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NestedInvoiceRequest(@NotBlank String name, @NotNull Boolean failInner) {
}
