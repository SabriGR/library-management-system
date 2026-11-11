package com.sabrigur.dto;

import java.math.BigDecimal;
import java.util.List;

/** Profile, active loans and unpaid fines of one member (FR-012). */
public record MemberDetailsResponse(UserResponse profile, List<BorrowingRecordResponse> activeLoans,
                                    BigDecimal unpaidFines) {
}
