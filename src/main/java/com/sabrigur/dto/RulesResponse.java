package com.sabrigur.dto;

import com.sabrigur.entity.LibraryRules;

import java.math.BigDecimal;

public record RulesResponse(int loanPeriodDays, int maxLoansPerMember, int maxRenewals, BigDecimal finePerDay) {

    public static RulesResponse from(LibraryRules r) {
        return new RulesResponse(r.getLoanPeriodDays(), r.getMaxLoansPerMember(), r.getMaxRenewals(), r.getFinePerDay());
    }
}
