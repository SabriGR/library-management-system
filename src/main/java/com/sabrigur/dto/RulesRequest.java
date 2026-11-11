package com.sabrigur.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RulesRequest(
        @Min(value = 1, message = "must be at least 1") int loanPeriodDays,
        @Min(value = 1, message = "must be at least 1") int maxLoansPerMember,
        @Min(value = 0, message = "must not be negative") int maxRenewals,
        @NotNull @DecimalMin(value = "0.00", message = "must not be negative") BigDecimal finePerDay) {
}
