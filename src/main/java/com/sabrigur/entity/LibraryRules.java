package com.sabrigur.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

/** The configurable library rules (FR-030). There is only one row, with id 1. */
@Entity
@Table(name = "library_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LibraryRules {

    public static final long SINGLETON_ID = 1L;

    @Id
    @Builder.Default
    private Long id = SINGLETON_ID;

    @Builder.Default
    private int loanPeriodDays = 14;

    @Builder.Default
    private int maxLoansPerMember = 5;

    @Builder.Default
    private int maxRenewals = 1;

    @Builder.Default
    private BigDecimal finePerDay = new BigDecimal("0.50");
}
